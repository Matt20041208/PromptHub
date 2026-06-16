package com.prompt.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.common.dto.UserDTO;
import com.prompt.user.dto.LoginDTO;
import com.prompt.user.dto.LoginResultDTO;
import com.prompt.user.dto.RegisterDTO;
import com.prompt.user.dto.UserUpdateDTO;
import com.prompt.user.entity.User;

public interface UserService extends IService<User> {
    void register(RegisterDTO dto);
    LoginResultDTO login(LoginDTO dto);
    UserDTO getUserInfo(Long userId);
    void updateUserInfo(Long userId, UserUpdateDTO dto);
    UserDTO getUserById(Long userId);
    void logout(String token);
    String buyVip(Long userId);
}
