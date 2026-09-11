package com.finance.workflow.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.framework.security.SecurityUtils;
import com.finance.workflow.domain.WfProcessInstance;
import com.finance.workflow.dto.WorkflowStartDTO;
import com.finance.workflow.dto.WorkflowTaskVO;
import com.finance.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审批流接口（W1，docs 7.1）：/workflow，权限 workflow:*。
 */
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /** 发起审批（一单一流程，幂等）。 */
    @OperLog(title = "审批发起审批", operType = OperType.OTHER)
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('workflow:instance:start')")
    public Result<WfProcessInstance> start(@Valid @RequestBody WorkflowStartDTO dto) {
        return Result.ok(workflowService.start(SecurityUtils.getCompanyCode(),
                dto.getBusinessType(), dto.getBusinessId(), dto.getApprover(), dto.getComment()));
    }

    /** 我的审批待办。 */
    @GetMapping("/todo")
    @PreAuthorize("hasAuthority('workflow:task:approve')")
    public Result<List<WorkflowTaskVO>> todo() {
        return Result.ok(workflowService.todo(SecurityUtils.getUsername()));
    }

    /** 审批通过。 */
    @OperLog(title = "审批审批", operType = OperType.AUDIT)
    @PutMapping("/task/{taskId}/approve")
    @PreAuthorize("hasAuthority('workflow:task:approve')")
    public Result<Void> approve(@PathVariable String taskId,
                                @RequestParam(required = false) String comment) {
        workflowService.approve(taskId, comment);
        return Result.ok();
    }

    /** 审批驳回。 */
    @OperLog(title = "审批驳回", operType = OperType.OTHER)
    @PutMapping("/task/{taskId}/reject")
    @PreAuthorize("hasAuthority('workflow:task:approve')")
    public Result<Void> reject(@PathVariable String taskId,
                               @RequestParam(required = false) String comment) {
        workflowService.reject(taskId, comment);
        return Result.ok();
    }

    /** 流程历史。 */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('workflow:instance:start')")
    public Result<List<WfProcessInstance>> history(@RequestParam String businessType,
                                                   @RequestParam Long businessId) {
        return Result.ok(workflowService.history(businessType, businessId));
    }
}
