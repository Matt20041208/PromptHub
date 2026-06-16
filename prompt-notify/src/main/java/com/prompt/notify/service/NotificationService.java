package com.prompt.notify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.prompt.common.result.PageResult;
import com.prompt.notify.dto.NotificationVO;
import com.prompt.notify.entity.Notification;

import java.util.Map;

public interface NotificationService extends IService<Notification> {
    void send(Long userId, String type, String title, String content, String bizType, Long bizId);
    void sendByTemplate(Long userId, String templateCode, Map<String, String> params, String bizType, Long bizId);
    PageResult<NotificationVO> listByUser(Long userId, long page, long size, Boolean unreadOnly);
    long getUnreadCount(Long userId);
    void markRead(Long id, Long userId);
    void markAllRead(Long userId);
}
