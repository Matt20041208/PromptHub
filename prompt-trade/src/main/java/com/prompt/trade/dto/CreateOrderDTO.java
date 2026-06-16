package com.prompt.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderDTO {
    @NotNull(message = "提示词ID不能为空")
    private Long promptId;
}
