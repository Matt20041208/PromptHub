package com.prompt.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private String orderNo;
    private String promptTitle;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private String sellerNickname;
    private String buyerNickname;
}
