package com.finance.system;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysMessage;
import com.finance.system.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 站内消息集成测试（W2，docs 5.4）：发送幂等、本人隔离、已读权限。
 */
@SpringBootTest
@ActiveProfiles("test")
class MessageServiceTest {

    @Autowired
    private MessageService messageService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long RECEIVER_A = 900001L;
    private static final long RECEIVER_B = 900002L;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("DELETE FROM sys_message");
    }

    @Test
    void send_thenListForReceiver() {
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_PLAN_DUE,
                "计划即将到期", "内容A", "PAYMENT_PLAN", 1001L, LocalDate.now());
        messageService.send("DEMO", RECEIVER_B, SysMessage.TYPE_AR_OVERDUE,
                "应收逾期", "内容B", "PAYMENT_PLAN", 1002L, LocalDate.now());

        PageResult<SysMessage> mine = messageService.listMessages("DEMO", RECEIVER_A, false, 1, 10);
        assertThat(mine.getTotal()).isEqualTo(1);
        assertThat(mine.getRecords().get(0).getReceiverId()).isEqualTo(RECEIVER_A);
        assertThat(mine.getRecords().get(0).getIsRead()).isZero();

        // 未读过滤
        PageResult<SysMessage> unread = messageService.listMessages("DEMO", RECEIVER_A, true, 1, 10);
        assertThat(unread.getTotal()).isEqualTo(1);
    }

    @Test
    void send_sameDaySameBiz_idempotent() {
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_PLAN_DUE,
                "标题", "内容", "PAYMENT_PLAN", 2001L, LocalDate.now());
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_PLAN_DUE,
                "标题", "内容", "PAYMENT_PLAN", 2001L, LocalDate.now());
        // 同单同日同类型 → 幂等仅 1 条（uq_sys_message_remind）
        assertThat(messageService.listMessages("DEMO", RECEIVER_A, false, 1, 10).getTotal())
                .isEqualTo(1);

        // 不同日期 → 可再提醒
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_PLAN_DUE,
                "标题", "内容", "PAYMENT_PLAN", 2001L, LocalDate.now().plusDays(1));
        assertThat(messageService.listMessages("DEMO", RECEIVER_A, false, 1, 10).getTotal())
                .isEqualTo(2);
    }

    @Test
    void markRead_ownMessageOnly() {
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_PLAN_DUE,
                "标题", "内容", "PAYMENT_PLAN", 3001L, LocalDate.now());
        SysMessage msg = messageService.listMessages("DEMO", RECEIVER_A, false, 1, 10)
                .getRecords().get(0);

        messageService.markRead("DEMO", RECEIVER_A, msg.getId());
        assertThat(messageService.countUnread("DEMO", RECEIVER_A)).isZero();
        assertThat(messageService.listMessages("DEMO", RECEIVER_A, true, 1, 10).getTotal())
                .isZero();

        // 他人不能标记
        assertThatThrownBy(() -> messageService.markRead("DEMO", RECEIVER_B, msg.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void markAllRead_clearsUnread() {
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_PLAN_DUE,
                "一", "内容", "PAYMENT_PLAN", 4001L, LocalDate.now());
        messageService.send("DEMO", RECEIVER_A, SysMessage.TYPE_AR_OVERDUE,
                "二", "内容", "PAYMENT_PLAN", 4002L, LocalDate.now());
        messageService.markAllRead("DEMO", RECEIVER_A);
        assertThat(messageService.countUnread("DEMO", RECEIVER_A)).isZero();
    }
}
