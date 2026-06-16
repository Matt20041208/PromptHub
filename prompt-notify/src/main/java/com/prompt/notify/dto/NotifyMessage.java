package com.prompt.notify.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NotifyMessage {
    private String type;
    private Long userId;
    private String orderNo;
    private String promptTitle;
    private BigDecimal amount;
}
