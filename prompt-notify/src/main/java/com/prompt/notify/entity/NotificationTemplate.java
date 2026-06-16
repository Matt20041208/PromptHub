package com.prompt.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notification_template")
public class NotificationTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String titleTemplate;
    private String contentTemplate;
    private String channel;
}
