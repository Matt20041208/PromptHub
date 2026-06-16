package com.prompt.notify.controller;

import com.prompt.common.constant.CommonConstants;
import com.prompt.common.result.PageResult;
import com.prompt.common.result.Result;
import com.prompt.notify.dto.NotificationVO;
import com.prompt.notify.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "通知列表、已读管理")
public class NotifyController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    @Operation(summary = "获取通知列表")
    public Result<PageResult<NotificationVO>> list(
            @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Boolean unreadOnly) {
        PageResult<NotificationVO> result = notificationService.listByUser(userId, page, size, unreadOnly);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public Result<Long> getUnreadCount(
            @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    public Result<Void> markRead(
            @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId,
            @PathVariable Long id) {
        notificationService.markRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记为已读")
    public Result<Void> markAllRead(
            @RequestHeader(CommonConstants.USER_ID_HEADER) Long userId) {
        notificationService.markAllRead(userId);
        return Result.success();
    }

    @PostMapping("/internal/send")
    @Operation(summary = "内部接口：发送通知")
    public Result<Void> internalSend(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String type = (String) body.get("type");
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String bizType = (String) body.getOrDefault("bizType", "ORDER");
        Long bizId = body.get("bizId") != null ? Long.valueOf(body.get("bizId").toString()) : userId;
        notificationService.send(userId, type, title, content, bizType, bizId);
        return Result.success();
    }
}
