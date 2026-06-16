package com.prompt.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.prompt.common.exception.NotFoundException;
import com.prompt.common.result.PageResult;
import com.prompt.notify.dto.NotificationVO;
import com.prompt.notify.entity.Notification;
import com.prompt.notify.entity.NotificationTemplate;
import com.prompt.notify.mapper.NotificationMapper;
import com.prompt.notify.mapper.NotificationTemplateMapper;
import com.prompt.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final NotificationTemplateMapper templateMapper;
    private final JavaMailSender mailSender;

    @Value("${notify.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    @Transactional
    public void send(Long userId, String type, String title, String content, String bizType, Long bizId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(notification);
        log.info("Notification sent to user {}: type={}, title={}", userId, type, title);
    }

    @Override
    public void sendByTemplate(Long userId, String templateCode, Map<String, String> params, String bizType, Long bizId) {
        NotificationTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplate>().eq(NotificationTemplate::getCode, templateCode));
        if (template == null) {
            throw new NotFoundException("通知模板不存在: " + templateCode);
        }

        String title = template.getTitleTemplate();
        String content = template.getContentTemplate();

        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                title = title.replace(placeholder, entry.getValue());
                content = content.replace(placeholder, entry.getValue());
            }
        }

        send(userId, "SYSTEM", title, content, bizType, bizId);

        if ("EMAIL".equalsIgnoreCase(template.getChannel()) && emailEnabled) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setSubject(title);
                message.setText(content);
                message.setTo("user" + userId + "@example.com");
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Failed to send email notification: {}", e.getMessage());
            }
        }
    }

    @Override
    public PageResult<NotificationVO> listByUser(Long userId, long page, long size, Boolean unreadOnly) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(Notification::getIsRead, 0);
        }
        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> queryPage = new Page<>(page, size);
        Page<Notification> result = baseMapper.selectPage(queryPage, wrapper);

        List<NotificationVO> records = result.getRecords().stream()
                .map(n -> BeanUtil.copyProperties(n, NotificationVO.class))
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), page, size);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    @Override
    @Transactional
    public void markRead(Long id, Long userId) {
        Notification notification = baseMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new NotFoundException("通知不存在");
        }
        notification.setIsRead(1);
        notification.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0);

        Notification update = new Notification();
        update.setIsRead(1);
        update.setUpdateTime(LocalDateTime.now());

        baseMapper.update(update, wrapper);
        log.info("All notifications marked as read for user {}", userId);
    }
}
