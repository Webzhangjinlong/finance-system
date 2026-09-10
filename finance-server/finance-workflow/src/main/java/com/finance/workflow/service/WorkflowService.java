package com.finance.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.core.exception.BusinessException;
import com.finance.framework.security.SecurityUtils;
import com.finance.workflow.domain.WfProcessInstance;
import com.finance.workflow.dto.WorkflowTaskVO;
import com.finance.workflow.mapper.WfProcessInstanceMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批流封装（W1，docs 7.1）：start / todo / approve / reject / history。
 *
 * <p>一期单人顺序审批（BPMN：singleApproval），审批人由发起时指定；
 * 通过/驳回由 WorkflowService 回调更新 wf_process_instance 状态；
 * business_type + business_id 唯一（uq_wf_business）保证一单一流程幂等。
 * approve/reject 返回业务流程记录，供业务模块（如 C2 合同）联动状态与后续动作。</p>
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    /** 流程定义 key（BPMN id）。 */
    public static final String PROCESS_KEY = "singleApproval";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final WfProcessInstanceMapper processInstanceMapper;

    public WorkflowService(RuntimeService runtimeService,
                           TaskService taskService,
                           WfProcessInstanceMapper processInstanceMapper) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.processInstanceMapper = processInstanceMapper;
    }

    /** 发起审批：启动 Flowable 流程 + 落 wf_process_instance（幂等，重复发起被拒）。 */
    @Transactional
    public WfProcessInstance start(String companyCode, String businessType, Long businessId,
                                   String approver, String comment) {
        WfProcessInstance exist = processInstanceMapper.selectOne(new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getBusinessType, businessType)
                .eq(WfProcessInstance::getBusinessId, businessId)
                .last("LIMIT 1"));
        if (exist != null) {
            throw new BusinessException("该业务单据已发起流程，禁止重复发起（状态：" + exist.getStatus() + "）");
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("approver", approver);
        vars.put("businessType", businessType);
        vars.put("businessId", businessId);
        vars.put("companyCode", companyCode);
        if (comment != null) {
            vars.put("startComment", comment);
        }
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, businessType + ":" + businessId, vars);

        WfProcessInstance record = new WfProcessInstance();
        record.setCompanyCode(companyCode);
        record.setProcessType("APPROVAL");
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setProcessDefKey(PROCESS_KEY);
        record.setProcessInstanceId(pi.getProcessInstanceId());
        record.setStatus("RUNNING");
        record.setCurrentApprover(approver);
        record.setStartedBy(SecurityUtils.getUsername());
        record.setStartedAt(OffsetDateTime.now());
        try {
            processInstanceMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该业务单据已发起流程（唯一约束兜底）", e);
        }
        log.info("发起审批：type={} bizId={} approver={} flowableId={}",
                businessType, businessId, approver, pi.getProcessInstanceId());
        return record;
    }

    /** 审批待办：某审批人名下的运行中任务（含业务信息）。 */
    public List<WorkflowTaskVO> todo(String approver) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(approver)
                .active()
                .orderByTaskCreateTime().desc()
                .list();
        return tasks.stream().map(t -> {
            WorkflowTaskVO vo = new WorkflowTaskVO();
            vo.setTaskId(t.getId());
            vo.setProcessInstanceId(t.getProcessInstanceId());
            vo.setAssignee(t.getAssignee());
            vo.setCreateTime(String.valueOf(t.getCreateTime()));
            WfProcessInstance record = processInstanceMapper.selectOne(
                    new LambdaQueryWrapper<WfProcessInstance>()
                            .eq(WfProcessInstance::getProcessInstanceId, t.getProcessInstanceId())
                            .last("LIMIT 1"));
            if (record != null) {
                vo.setBusinessType(record.getBusinessType());
                vo.setBusinessId(record.getBusinessId());
                vo.setStatus(record.getStatus());
            }
            return vo;
        }).toList();
    }

    /** 审批通过：完成当前任务（approved=true），流程结束，业务记录 APPROVED；返回业务流程记录。 */
    @Transactional
    public WfProcessInstance approve(String taskId, String comment) {
        return complete(taskId, true, comment);
    }

    /** 审批驳回：完成当前任务（approved=false），流程结束，业务记录 REJECTED；返回业务流程记录。 */
    @Transactional
    public WfProcessInstance reject(String taskId, String comment) {
        return complete(taskId, false, comment);
    }

    /** 审批历史：某业务单据的流程记录。 */
    public List<WfProcessInstance> history(String businessType, Long businessId) {
        return processInstanceMapper.selectList(new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getBusinessType, businessType)
                .eq(WfProcessInstance::getBusinessId, businessId)
                .orderByDesc(WfProcessInstance::getStartedAt));
    }

    // ==================== 内部实现 ====================

    private WfProcessInstance complete(String taskId, boolean approved, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException("审批任务不存在或已完成");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", approved);
        if (comment != null) {
            vars.put("comment", comment);
        }
        taskService.complete(taskId, vars);

        WfProcessInstance record = processInstanceMapper.selectOne(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getProcessInstanceId, task.getProcessInstanceId())
                        .last("LIMIT 1"));
        if (record != null) {
            WfProcessInstance update = new WfProcessInstance();
            update.setId(record.getId());
            update.setStatus(approved ? "APPROVED" : "REJECTED");
            update.setCurrentApprover(null);
            update.setFinishedAt(OffsetDateTime.now());
            processInstanceMapper.updateById(update);
            log.info("审批{}：type={} bizId={} taskId={}",
                    approved ? "通过" : "驳回", record.getBusinessType(), record.getBusinessId(), taskId);
        }
        return record;
    }
}
