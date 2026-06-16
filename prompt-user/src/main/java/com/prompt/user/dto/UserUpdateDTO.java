package com.prompt.user.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String bio;
}
