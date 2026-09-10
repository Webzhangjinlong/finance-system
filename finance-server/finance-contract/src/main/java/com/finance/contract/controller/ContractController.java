package com.finance.contract.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.dto.ContractDTO;
import com.finance.contract.dto.ContractSubmitDTO;
import com.finance.contract.service.ContractService;
import com.finance.framework.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 合同管理接口（C1/C2，docs 5.1/5.2）：/contract。
 *
 * <p>台账维护权限 contract:contract:*；审批动作复用 W1 待办（/workflow/todo 取任务，
 * 审批完成走本控制器 approve/reject 以联动合同状态与计划生成），权限 workflow:task:approve。</p>
 */
@RestController
@RequestMapping("/contract")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /** 合同分页。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('contract:contract:list')")
    public Result<PageResult<CtrContract>> page(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(contractService.page(SecurityUtils.getCompanyCode(), keyword, status, page, size));
    }

    /** 合同详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contract:contract:list')")
    public Result<CtrContract> detail(@PathVariable Long id) {
        return Result.ok(contractService.getById(SecurityUtils.getCompanyCode(), id));
    }

    /** 登记合同（DRAFT，编号自动生成）。 */
    @PostMapping
    @PreAuthorize("hasAuthority('contract:contract:add')")
    public Result<CtrContract> create(@Valid @RequestBody ContractDTO dto) {
        return Result.ok(contractService.create(SecurityUtils.getCompanyCode(), dto));
    }

    /** 修改合同（仅 DRAFT）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('contract:contract:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContractDTO dto) {
        contractService.update(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    /** 删除合同（删除保护：仅 DRAFT）。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('contract:contract:del')")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.delete(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    /** 作废合同（已通过/履约中/已完成 → 已终止）。 */
    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('contract:contract:void')")
    public Result<Void> voidContract(@PathVariable Long id,
                                     @RequestParam(required = false) String reason) {
        contractService.voidContract(SecurityUtils.getCompanyCode(), id, reason);
        return Result.ok();
    }

    /** 提交审批（DRAFT → APPROVING + 发起流程）。 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('contract:contract:submit')")
    public Result<Void> submit(@PathVariable Long id, @Valid @RequestBody ContractSubmitDTO dto) {
        contractService.submit(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    /** 审批通过（联动合同生效 + 生成收付款计划）。 */
    @PutMapping("/task/{taskId}/approve")
    @PreAuthorize("hasAuthority('workflow:task:approve')")
    public Result<Void> approve(@PathVariable String taskId, @RequestParam(required = false) String comment) {
        contractService.approveTask(taskId, comment);
        return Result.ok();
    }

    /** 审批驳回（合同回 DRAFT 可修改重提）。 */
    @PutMapping("/task/{taskId}/reject")
    @PreAuthorize("hasAuthority('workflow:task:approve')")
    public Result<Void> reject(@PathVariable String taskId, @RequestParam(required = false) String comment) {
        contractService.rejectTask(taskId, comment);
        return Result.ok();
    }

    /** 收付款计划列表。 */
    @GetMapping("/{id}/plans")
    @PreAuthorize("hasAuthority('contract:contract:list')")
    public Result<List<CtrPaymentPlan>> plans(@PathVariable Long id) {
        return Result.ok(contractService.listPlans(SecurityUtils.getCompanyCode(), id));
    }
}
