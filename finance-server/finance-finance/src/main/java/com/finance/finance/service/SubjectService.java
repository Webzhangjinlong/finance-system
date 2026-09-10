package com.finance.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinSubject;
import com.finance.finance.enums.SubjectDirection;
import com.finance.finance.enums.SubjectType;
import com.finance.finance.mapper.FinSubjectMapper;
import com.finance.finance.mapper.FinVoucherEntryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 科目管理服务（F1，docs 4.1）：树形、新增、修改、删除保护。
 *
 * <p>硬约束 3（仅末级科目可记账由凭证服务校验）、2（公司隔离）：
 * 所有查询/写操作按 companyCode 过滤；编码唯一（DB 唯一约束兜底）。</p>
 */
@Service
public class SubjectService {

    /** 科目编码必须为纯数字（层级由编码前缀表达）。 */
    private static final String CODE_PATTERN = "\\d+";

    private final FinSubjectMapper subjectMapper;
    private final FinVoucherEntryMapper voucherEntryMapper;

    public SubjectService(FinSubjectMapper subjectMapper, FinVoucherEntryMapper voucherEntryMapper) {
        this.subjectMapper = subjectMapper;
        this.voucherEntryMapper = voucherEntryMapper;
    }

    /** 树形查询（docs 4.1：按编码层级树形展示）。 */
    public List<FinSubject> tree(String companyCode) {
        List<FinSubject> all = subjectMapper.selectList(new LambdaQueryWrapper<FinSubject>()
                .eq(FinSubject::getCompanyCode, companyCode)
                .orderByAsc(FinSubject::getSubjectCode));
        Map<Long, List<FinSubject>> childrenMap = new HashMap<>();
        for (FinSubject s : all) {
            childrenMap.computeIfAbsent(s.getParentId(), k -> new ArrayList<>()).add(s);
        }
        List<FinSubject> roots = childrenMap.getOrDefault(0L, new ArrayList<>());
        roots.sort(Comparator.comparing(FinSubject::getSubjectCode));
        for (FinSubject root : roots) {
            buildChildren(root, childrenMap);
        }
        return roots;
    }

    private void buildChildren(FinSubject node, Map<Long, List<FinSubject>> childrenMap) {
        List<FinSubject> children = childrenMap.getOrDefault(node.getId(), new ArrayList<>());
        children.sort(Comparator.comparing(FinSubject::getSubjectCode));
        node.setChildren(children);
        for (FinSubject child : children) {
            buildChildren(child, childrenMap);
        }
    }

    /** 新增科目。 */
    @Transactional
    public void add(String companyCode, FinSubject subject) {
        validateNew(companyCode, subject);
        subject.setCompanyCode(companyCode);
        subject.setStatus(subject.getStatus() == null ? "ACTIVE" : subject.getStatus());
        subject.setIsLeaf(1);
        if (subject.getParentId() != null && subject.getParentId() > 0) {
            FinSubject parent = getById(companyCode, subject.getParentId());
            if (parent == null) {
                throw new BusinessException("父科目不存在");
            }
            subject.setSubjectLevel(parent.getSubjectLevel() + 1);
            // 父科目升级为非末级
            if (parent.getIsLeaf() != null && parent.getIsLeaf() == 1) {
                FinSubject update = new FinSubject();
                update.setId(parent.getId());
                update.setIsLeaf(0);
                subjectMapper.updateById(update);
            }
        } else {
            subject.setParentId(0L);
            subject.setSubjectLevel(1);
        }
        try {
            subjectMapper.insert(subject);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_fin_subject")) {
                throw new BusinessException("科目编码在该公司已存在");
            }
            throw e;
        }
    }

    /** 修改科目（仅名称/类别/方向/状态；编码禁止修改）。 */
    @Transactional
    public void update(String companyCode, Long id, FinSubject subject) {
        FinSubject exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("科目不存在");
        }
        if (StringUtils.hasText(subject.getSubjectCode())
                && !subject.getSubjectCode().equals(exist.getSubjectCode())) {
            throw new BusinessException("科目编码禁止修改");
        }
        boolean hasAmount = hasVoucherEntries(id);
        if (hasAmount && subject.getSubjectType() != null
                && !subject.getSubjectType().equals(exist.getSubjectType())) {
            throw new BusinessException("已有发生额的科目禁止修改类别");
        }
        if (hasAmount && subject.getDirection() != null
                && !subject.getDirection().equals(exist.getDirection())) {
            throw new BusinessException("已有发生额的科目禁止修改方向");
        }
        FinSubject update = new FinSubject();
        update.setId(id);
        update.setSubjectName(subject.getSubjectName());
        update.setSubjectType(subject.getSubjectType());
        update.setDirection(subject.getDirection());
        update.setStatus(subject.getStatus());
        subjectMapper.updateById(update);
    }

    /** 删除科目（删除保护：存在子科目或发生额禁止删除）。 */
    @Transactional
    public void delete(String companyCode, Long id) {
        FinSubject exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("科目不存在");
        }
        Long childCount = subjectMapper.selectCount(new LambdaQueryWrapper<FinSubject>()
                .eq(FinSubject::getCompanyCode, companyCode)
                .eq(FinSubject::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("存在子科目，禁止删除");
        }
        if (hasVoucherEntries(id)) {
            throw new BusinessException("已有发生额，禁止删除（可停用）");
        }
        subjectMapper.deleteById(id);
    }

    private boolean hasVoucherEntries(Long subjectId) {
        return voucherEntryMapper.selectCount(
                new LambdaQueryWrapper<com.finance.finance.domain.FinVoucherEntry>()
                        .eq(com.finance.finance.domain.FinVoucherEntry::getSubjectId, subjectId)) > 0;
    }

    private void validateNew(String companyCode, FinSubject subject) {
        if (!StringUtils.hasText(subject.getSubjectCode())) {
            throw new BusinessException("科目编码不能为空");
        }
        if (!subject.getSubjectCode().matches(CODE_PATTERN)) {
            throw new BusinessException("科目编码必须为纯数字");
        }
        if (!StringUtils.hasText(subject.getSubjectName())) {
            throw new BusinessException("科目名称不能为空");
        }
        if (!SubjectType.isValid(subject.getSubjectType())) {
            throw new BusinessException("科目类别不合法");
        }
        if (!SubjectDirection.isValid(subject.getDirection())) {
            throw new BusinessException("科目方向不合法");
        }
        Long exist = subjectMapper.selectCount(new LambdaQueryWrapper<FinSubject>()
                .eq(FinSubject::getCompanyCode, companyCode)
                .eq(FinSubject::getSubjectCode, subject.getSubjectCode()));
        if (exist != null && exist > 0) {
            throw new BusinessException("科目编码在该公司已存在");
        }
    }

    private FinSubject getById(String companyCode, Long id) {
        return subjectMapper.selectOne(new LambdaQueryWrapper<FinSubject>()
                .eq(FinSubject::getId, id)
                .eq(FinSubject::getCompanyCode, companyCode)
                .last("LIMIT 1"));
    }

    /** 当前公司全部有效科目（供凭证分录校验使用）。 */
    public Map<String, FinSubject> codeIndex(String companyCode) {
        List<FinSubject> all = subjectMapper.selectList(new LambdaQueryWrapper<FinSubject>()
                .eq(FinSubject::getCompanyCode, companyCode)
                .eq(FinSubject::getStatus, "ACTIVE"));
        return all.stream().collect(Collectors.toMap(FinSubject::getSubjectCode, s -> s, (a, b) -> a));
    }
}
