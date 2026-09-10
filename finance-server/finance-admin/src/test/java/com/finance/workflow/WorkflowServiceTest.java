package com.finance.workflow;

import com.finance.common.core.exception.BusinessException;
import com.finance.workflow.domain.WfProcessInstance;
import com.finance.workflow.dto.WorkflowTaskVO;
import com.finance.workflow.mapper.WfProcessInstanceMapper;
import com.finance.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 审批流集成测试（W1，docs 7.1）：真实 Flowable 引擎 + 测试库，全流程验证。
 *
 * <p>覆盖：发起→待办→通过（APPROVED）/ 驳回（REJECTED）、幂等防重复发起、流程历史。
 * Flowable 运行表（ACT_*）由引擎自管理且独立事务（Spring 测试回滚不覆盖），
 * 故 @BeforeEach 清理运行态任务与流程记录，保证反复运行可重。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class WorkflowServiceTest {

    private static final AtomicLong SEQ = new AtomicLong(10000);

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WfProcessInstanceMapper processInstanceMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanFlowableRuntime() {
        // 物理删除：逻辑删除无法释放 uq_wf_business 唯一索引，会误伤幂等插入
        jdbcTemplate.execute("DELETE FROM wf_process_instance");
        jdbcTemplate.execute("DELETE FROM act_ru_identitylink");
        jdbcTemplate.execute("DELETE FROM act_ru_variable");
        jdbcTemplate.execute("DELETE FROM act_ru_task");
        jdbcTemplate.execute("DELETE FROM act_ru_execution");
    }

    @Test
    void fullFlow_startTodoApprove() {
        String bizType = "CONTRACT";
        long bizId = SEQ.incrementAndGet();
        String approver = "approver-" + bizId;

        WfProcessInstance started = workflowService.start("DEMO", bizType, bizId, approver, null);
        assertThat(started.getStatus()).isEqualTo("RUNNING");
        assertThat(started.getCurrentApprover()).isEqualTo(approver);

        List<WorkflowTaskVO> todo = workflowService.todo(approver);
        assertThat(todo).hasSize(1);
        WorkflowTaskVO task = todo.get(0);
        assertThat(task.getBusinessId()).isEqualTo(bizId);
        assertThat(task.getBusinessType()).isEqualTo(bizType);

        workflowService.approve(task.getTaskId(), "同意");

        WfProcessInstance after = processInstanceMapper.selectById(started.getId());
        assertThat(after.getStatus()).isEqualTo("APPROVED");
        assertThat(after.getFinishedAt()).isNotNull();
        // 流程已结束，待办清空
        assertThat(workflowService.todo(approver)).isEmpty();
    }

    @Test
    void fullFlow_startTodoReject() {
        String bizType = "CONTRACT";
        long bizId = SEQ.incrementAndGet();
        String approver = "approver-" + bizId;

        WfProcessInstance started = workflowService.start("DEMO", bizType, bizId, approver, null);
        List<WorkflowTaskVO> todo = workflowService.todo(approver);
        assertThat(todo).hasSize(1);

        workflowService.reject(todo.get(0).getTaskId(), "不同意");

        WfProcessInstance after = processInstanceMapper.selectById(started.getId());
        assertThat(after.getStatus()).isEqualTo("REJECTED");
        assertThat(workflowService.todo(approver)).isEmpty();
    }

    @Test
    void start_duplicate_rejected() {
        String bizType = "EXPENSE";
        long bizId = SEQ.incrementAndGet();
        String approver = "approver-" + bizId;

        workflowService.start("DEMO", bizType, bizId, approver, null);
        assertThatThrownBy(() -> workflowService.start("DEMO", bizType, bizId, approver, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已发起流程");
    }

    @Test
    void approve_unknownTask_rejected() {
        assertThatThrownBy(() -> workflowService.approve("non-existent-task", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在或已完成");
    }

    @Test
    void history_returnsStartedAndFinished() {
        String bizType = "CONTRACT";
        long bizId = SEQ.incrementAndGet();
        String approver = "approver-" + bizId;

        workflowService.start("DEMO", bizType, bizId, approver, null);
        List<WfProcessInstance> history = workflowService.history(bizType, bizId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getBusinessId()).isEqualTo(bizId);
    }
}
