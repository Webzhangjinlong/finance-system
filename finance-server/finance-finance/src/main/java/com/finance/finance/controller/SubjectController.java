package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.finance.domain.FinSubject;
import com.finance.finance.service.SubjectService;
import com.finance.framework.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 科目管理接口（F1，docs 4.1）：/finance/subject，权限 finance:subject:*。
 */
@RestController
@RequestMapping("/finance/subject")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    /** 科目树。 */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('finance:subject:list')")
    public Result<List<FinSubject>> tree() {
        return Result.ok(subjectService.tree(SecurityUtils.getCompanyCode()));
    }

    /** 新增科目。 */
    @PostMapping
    @PreAuthorize("hasAuthority('finance:subject:add')")
    @OperLog(title = "科目新增", operType = OperType.INSERT)
    public Result<Void> add(@RequestBody FinSubject subject) {
        subjectService.add(SecurityUtils.getCompanyCode(), subject);
        return Result.ok();
    }

    /** 修改科目（编码禁止修改）。 */
    @OperLog(title = "科目修改", operType = OperType.UPDATE)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:subject:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody FinSubject subject) {
        subjectService.update(SecurityUtils.getCompanyCode(), id, subject);
        return Result.ok();
    }

    /** 删除科目（子科目/发生额保护）。 */
    @OperLog(title = "科目删除", operType = OperType.DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:subject:del')")
    public Result<Void> delete(@PathVariable Long id) {
        subjectService.delete(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }
}
