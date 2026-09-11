package com.finance.hr.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.common.core.domain.PageResult;
import com.finance.framework.security.SecurityUtils;
import com.finance.hr.domain.HrSocialDetail;
import com.finance.hr.domain.HrSocialRule;
import com.finance.hr.dto.SocialRuleDTO;
import com.finance.hr.service.SocialInsuranceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 五险一金接口（批次 A）：/hr/social。
 *
 * <p>规则配置化（费率/基数上下限）+ 智能核算（基数 clamp + 个人/单位自动计算）+ 缴费明细分页。</p>
 */
@RestController
@RequestMapping("/hr/social")
public class SocialInsuranceController {

    private final SocialInsuranceService socialInsuranceService;

    public SocialInsuranceController(SocialInsuranceService socialInsuranceService) {
        this.socialInsuranceService = socialInsuranceService;
    }

    @GetMapping("/rule/page")
    @PreAuthorize("hasAuthority('hr:social:list')")
    public Result<PageResult<HrSocialRule>> rulePage(@RequestParam(required = false) String socialType,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
        return Result.ok(socialInsuranceService.pageRules(SecurityUtils.getCompanyCode(), socialType, page, size));
    }

    @OperLog(title = "社保公积金规则保存", operType = OperType.INSERT)
    @PostMapping("/rule")
    @PreAuthorize("hasAuthority('hr:social:rule:save')")
    public Result<Long> saveRule(@Valid @RequestBody SocialRuleDTO dto) {
        return Result.ok(socialInsuranceService.saveRule(SecurityUtils.getCompanyCode(), dto));
    }

    @OperLog(title = "社保公积金规则删除", operType = OperType.DELETE)
    @DeleteMapping("/rule/{id}")
    @PreAuthorize("hasAuthority('hr:social:rule:del')")
    public Result<Void> deleteRule(@PathVariable Long id) {
        socialInsuranceService.deleteRule(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    @OperLog(title = "五险一金缴费核算", operType = OperType.OTHER)
    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('hr:social:calculate')")
    public Result<Integer> calculate(@RequestParam int year,
                                     @RequestParam int month,
                                     @RequestParam(required = false) Long employeeId) {
        return Result.ok(socialInsuranceService.calculate(SecurityUtils.getCompanyCode(), year, month, employeeId));
    }

    @GetMapping("/detail/page")
    @PreAuthorize("hasAuthority('hr:social:detail:list')")
    public Result<PageResult<HrSocialDetail>> detailPage(@RequestParam(required = false) Long employeeId,
                                                         @RequestParam(required = false) Integer year,
                                                         @RequestParam(required = false) Integer month,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size) {
        return Result.ok(socialInsuranceService.pageDetails(SecurityUtils.getCompanyCode(), employeeId, year, month, page, size));
    }
}
