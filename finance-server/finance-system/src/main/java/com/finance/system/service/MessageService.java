package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysMessage;
import com.finance.system.mapper.SysMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 站内消息服务（W2/W4，docs 5.4）。
 *
 * <p>send 幂等：同 company+receiver+type+business+remind_date 仅一条
 * （uq_sys_message_remind 唯一约束 + DuplicateKey 吞并）；
 * 查询/已读仅限接收人本人（receiver_id + company_code 隔离）。</p>
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final SysMessageMapper messageMapper;

    public MessageService(SysMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /** 发送站内消息（幂等：同单同日同类型不重复）。 */
    @Transactional
    public void send(String companyCode, Long receiverId, String messageType,
                     String title, String content, String businessType,
                     Long businessId, LocalDate remindDate) {
        SysMessage msg = new SysMessage();
        msg.setCompanyCode(companyCode);
        msg.setReceiverId(receiverId);
        msg.setMessageType(messageType);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setBusinessType(businessType);
        msg.setBusinessId(businessId);
        msg.setRemindDate(remindDate);
        try {
            messageMapper.insert(msg);
            log.info("站内消息发送：receiver={} type={} biz={}:{} remindDate={}",
                    receiverId, messageType, businessType, businessId, remindDate);
        } catch (DuplicateKeyException e) {
            // 同单同日已提醒过 → 幂等跳过（L35 教训：唯一约束吞并，不重复入库）
            log.info("站内消息已存在（幂等跳过）：biz={}:{} remindDate={}",
                    businessType, businessId, remindDate);
        }
    }

    /** 消息分页（仅本人，支持未读过滤；未读优先）。 */
    public PageResult<SysMessage> listMessages(String companyCode, Long receiverId,
                                               boolean unreadOnly, long page, long size) {
        Page<SysMessage> p = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getCompanyCode, companyCode)
                        .eq(SysMessage::getReceiverId, receiverId)
                        .eq(unreadOnly, SysMessage::getIsRead, 0)
                        .orderByAsc(SysMessage::getIsRead)
                        .orderByDesc(SysMessage::getCreateTime));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 未读消息数（首页角标）。 */
    public long countUnread(String companyCode, Long receiverId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getCompanyCode, companyCode)
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getIsRead, 0));
    }

    /** 标记已读（仅本人消息）。 */
    @Transactional
    public void markRead(String companyCode, Long receiverId, Long messageId) {
        SysMessage msg = messageMapper.selectById(messageId);
        if (msg == null || !companyCode.equals(msg.getCompanyCode())
                || !receiverId.equals(msg.getReceiverId())) {
            throw new BusinessException("消息不存在");
        }
        LambdaUpdateWrapper<SysMessage> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysMessage::getId, messageId)
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getReadAt, OffsetDateTime.now());
        messageMapper.update(null, uw);
    }

    /** 全部已读。 */
    @Transactional
    public void markAllRead(String companyCode, Long receiverId) {
        LambdaUpdateWrapper<SysMessage> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysMessage::getCompanyCode, companyCode)
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getReadAt, OffsetDateTime.now());
        messageMapper.update(null, uw);
    }
}
