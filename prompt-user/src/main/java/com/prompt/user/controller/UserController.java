package com.prompt.user.controller;

import com.prompt.common.constant.CommonConstants;
import com.prompt.common.dto.UserDTO;
import com.prompt.common.result.Result;
import com.prompt.user.dto.LoginDTO;
import com.prompt.user.dto.LoginResultDTO;
import com.prompt.user.dto.RegisterDTO;
import com.prompt.user.dto.UserUpdateDTO;
import com.prompt.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginDTO dto) {
        LoginResultDTO result = userService.login(dto);
        return Result.success(result);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserDTO> getUserInfo(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        UserDTO userDTO = userService.getUserInfo(userId);
        return Result.success(userDTO);
    }

    @PutMapping("/info")
    @Operation(summary = "更新当前用户信息")
    public Result<Void> updateUserInfo(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
                                        @RequestBody UserUpdateDTO dto) {
        userService.updateUserInfo(userId, dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户信息")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserById(id);
        return Result.success(userDTO);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.logout(token);
        }
        return Result.success();
    }

    @PostMapping("/vip/buy")
    @Operation(summary = "秒杀购买VIP")
    public Result<String> buyVip(@RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        String result = userService.buyVip(userId);
        return Result.success(result);
    }
}
