package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.framework.security.SecurityUtils;
import com.finance.system.domain.SysMessage;
import com.finance.system.service.MessageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息接口（W2，docs 5.4）：/message。
 *
 * <p>仅登录用户本人可见（receiver_id 恒为当前用户）；消息中心菜单 1506
 * 权限 system:message:list；未读角标供首页待办聚合使用。</p>
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 消息分页（本人，可过滤未读）。 */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:message:list')")
    public Result<PageResult<SysMessage>> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.ok(messageService.listMessages(
                SecurityUtils.getCompanyCode(), SecurityUtils.getUserId(), unreadOnly, page, size));
    }

    /** 未读消息数（首页角标）。 */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('system:message:list')")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.countUnread(
                SecurityUtils.getCompanyCode(), SecurityUtils.getUserId()));
    }

    /** 标记单条已读（仅本人）。 */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('system:message:list')")
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(SecurityUtils.getCompanyCode(), SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    /** 全部已读。 */
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('system:message:list')")
    public Result<Void> readAll() {
        messageService.markAllRead(SecurityUtils.getCompanyCode(), SecurityUtils.getUserId());
        return Result.ok();
    }
}
