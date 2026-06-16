package com.prompt.user.dto;

import com.prompt.common.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResultDTO {
    private String token;
    private UserDTO user;
}
