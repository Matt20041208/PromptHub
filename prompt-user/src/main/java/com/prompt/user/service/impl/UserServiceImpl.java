package com.prompt.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.json.JSONUtil;
import com.prompt.common.constant.CommonConstants;
import com.prompt.common.dto.UserDTO;
import com.prompt.common.exception.BusinessException;
import com.prompt.common.exception.NotFoundException;
import com.prompt.common.util.JwtUtil;
import com.prompt.user.dto.LoginDTO;
import com.prompt.user.dto.LoginResultDTO;
import com.prompt.user.dto.RegisterDTO;
import com.prompt.user.dto.UserUpdateDTO;
import com.prompt.user.entity.User;
import com.prompt.user.mapper.UserMapper;
import com.prompt.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        Long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(CommonConstants.ROLE_USER);
        user.setStatus(Integer.valueOf(CommonConstants.USER_STATUS_NORMAL));
        baseMapper.insert(user);
    }

    @Override
    public LoginResultDTO login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = baseMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!Integer.valueOf(CommonConstants.USER_STATUS_NORMAL).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        UserDTO userDTO = toUserDTO(user);
        return new LoginResultDTO(token, userDTO);
    }

    @Override
    public UserDTO getUserInfo(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return toUserDTO(user);
    }

    @Override
    @Transactional
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        baseMapper.updateById(user);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return toUserDTO(user);
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setVip(user.getVip());
        return dto;
    }

    @Override
    public void logout(String token) {
        try {
            var claims = JwtUtil.parseToken(token);
            long expiration = claims.getExpiration().getTime() / 1000;
            long remainingSeconds = expiration - Instant.now().getEpochSecond();
            if (remainingSeconds > 0) {
                stringRedisTemplate.opsForValue().set("blacklist:" + token, "1", Duration.ofSeconds(remainingSeconds));
            }
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public String buyVip(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (Boolean.TRUE.equals(user.getVip())) throw new BusinessException("您已是VIP用户");

        // Redis 秒杀：扣库存（原子操作）
        Long stock = stringRedisTemplate.opsForValue().decrement("vip:stock");
        if (stock == null || stock < 0) {
            // 库存没了，恢复计数
            stringRedisTemplate.opsForValue().increment("vip:stock");
            throw new BusinessException("VIP已售罄，请等待下一轮秒杀");
        }

        // 分布式锁：防同一用户重复购买
        String lockKey = "vip:lock:" + userId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) throw new BusinessException("处理中，请勿重复提交");

        try {
            user.setVip(true);
            baseMapper.updateById(user);

            // MQ 消息：异步通知 + 异步更新缓存
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "VIP_BUY");
            msg.put("userId", userId);
            msg.put("username", user.getUsername());
            msg.put("amount", 999);
            rabbitTemplate.convertAndSend("trade.exchange", "vip.bought", JSONUtil.toJsonStr(msg));

            return "恭喜！您已成为超级VIP，所有提示词免费查看";
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }
}
