package com.prompt.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.prompt.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class Review extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptId;
    private Long userId;
    private Long orderId;
    private Integer rating;
    private String content;

    @TableField(exist = false)
    private LocalDateTime updateTime;
}
