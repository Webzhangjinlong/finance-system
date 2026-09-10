package com.finance.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinAp;
import com.finance.finance.domain.FinAr;
import com.finance.finance.domain.vo.AgingVO;
import com.finance.finance.mapper.FinApMapper;
import com.finance.finance.mapper.FinArMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 应收/应付服务（C3 联动，docs 5.3）：计划到期生成 AR/AP（幂等）+ 收付款核销回写（超额拦截）。
 *
 * <p>本服务只操作 fin_ar / fin_ap（财务域），不触碰合同/计划表；
 * 计划状态回写由 contract 模块编排（走本服务方法，跨模块不直调 Mapper，R16）。</p>
 */
@Service
public class ArApService {

    private static final Logger log = LoggerFactory.getLogger(ArApService.class);

    private final FinArMapper arMapper;
    private final FinApMapper apMapper;

    public ArApService(FinArMapper arMapper, FinApMapper apMapper) {
        this.arMapper = arMapper;
        this.apMapper = apMapper;
    }

    /** 由收付款计划生成应收账款（幂等：company_code+plan_id 唯一，重复调用返回已存在单）。 */
    @Transactional
    public FinAr generateArFromPlan(String companyCode, Long planId, Long contractId,
                                    BigDecimal amount, LocalDate dueDate, String customerName) {
        FinAr exist = arMapper.selectOne(new LambdaQueryWrapper<FinAr>()
                .eq(FinAr::getCompanyCode, companyCode)
                .eq(FinAr::getPlanId, planId)
                .last("LIMIT 1"));
        if (exist != null) {
            return exist;
        }
        FinAr ar = new FinAr();
        ar.setCompanyCode(companyCode);
        ar.setArNo(nextArNo(companyCode));
        ar.setContractId(contractId);
        ar.setPlanId(planId);
        ar.setCustomerName(customerName);
        ar.setAmount(amount);
        ar.setReceivedAmount(BigDecimal.ZERO);
        ar.setStatus(FinAr.STATUS_OPEN);
        ar.setDueDate(dueDate);
        try {
            arMapper.insert(ar);
        } catch (DuplicateKeyException e) {
            // 并发下 uq_fin_ar_plan 兜底：查询已存在记录返回
            return arMapper.selectOne(new LambdaQueryWrapper<FinAr>()
                    .eq(FinAr::getCompanyCode, companyCode)
                    .eq(FinAr::getPlanId, planId)
                    .last("LIMIT 1"));
        }
        log.info("生成应收单：planId={} arNo={} amount={} due={}", planId, ar.getArNo(), amount, dueDate);
        return ar;
    }

    /** 由收付款计划生成应付账款（幂等：company_code+plan_id 唯一，重复调用返回已存在单）。 */
    @Transactional
    public FinAp generateApFromPlan(String companyCode, Long planId, Long contractId,
                                    BigDecimal amount, LocalDate dueDate, String supplierName) {
        FinAp exist = apMapper.selectOne(new LambdaQueryWrapper<FinAp>()
                .eq(FinAp::getCompanyCode, companyCode)
                .eq(FinAp::getPlanId, planId)
                .last("LIMIT 1"));
        if (exist != null) {
            return exist;
        }
        FinAp ap = new FinAp();
        ap.setCompanyCode(companyCode);
        ap.setApNo(nextApNo(companyCode));
        ap.setContractId(contractId);
        ap.setPlanId(planId);
        ap.setSupplierName(supplierName);
        ap.setAmount(amount);
        ap.setPaidAmount(BigDecimal.ZERO);
        ap.setStatus(FinAp.STATUS_OPEN);
        ap.setDueDate(dueDate);
        try {
            apMapper.insert(ap);
        } catch (DuplicateKeyException e) {
            return apMapper.selectOne(new LambdaQueryWrapper<FinAp>()
                    .eq(FinAp::getCompanyCode, companyCode)
                    .eq(FinAp::getPlanId, planId)
                    .last("LIMIT 1"));
        }
        log.info("生成应付单：planId={} apNo={} amount={} due={}", planId, ap.getApNo(), amount, dueDate);
        return ap;
    }

    /** 收款核销：回写应收单 received_amount/status（超额拦截：服务校验 + DB CHECK 兜底）。 */
    @Transactional
    public void applyReceipt(String companyCode, Long arId, BigDecimal amount) {
        FinAr ar = arMapper.selectById(arId);
        if (ar == null || !companyCode.equals(ar.getCompanyCode())) {
            throw new BusinessException("应收单不存在");
        }
        BigDecimal received = ar.getReceivedAmount() == null ? BigDecimal.ZERO : ar.getReceivedAmount();
        BigDecimal after = received.add(amount);
        if (after.compareTo(ar.getAmount()) > 0) {
            throw new BusinessException("收款金额超过应收余额，禁止超额核销（应收 " + ar.getAmount()
                    + "，已收 " + received + "，本次 " + amount + "）");
        }
        FinAr update = new FinAr();
        update.setId(arId);
        update.setReceivedAmount(after);
        update.setStatus(after.compareTo(ar.getAmount()) >= 0 ? FinAr.STATUS_SETTLED : FinAr.STATUS_PARTIAL);
        arMapper.updateById(update);
    }

    /** 付款核销：回写应付单 paid_amount/status（超额拦截：服务校验 + DB CHECK 兜底）。 */
    @Transactional
    public void applyPayment(String companyCode, Long apId, BigDecimal amount) {
        FinAp ap = apMapper.selectById(apId);
        if (ap == null || !companyCode.equals(ap.getCompanyCode())) {
            throw new BusinessException("应付单不存在");
        }
        BigDecimal paid = ap.getPaidAmount() == null ? BigDecimal.ZERO : ap.getPaidAmount();
        BigDecimal after = paid.add(amount);
        if (after.compareTo(ap.getAmount()) > 0) {
            throw new BusinessException("付款金额超过应付余额，禁止超额核销（应付 " + ap.getAmount()
                    + "，已付 " + paid + "，本次 " + amount + "）");
        }
        FinAp update = new FinAp();
        update.setId(apId);
        update.setPaidAmount(after);
        update.setStatus(after.compareTo(ap.getAmount()) >= 0 ? FinAp.STATUS_SETTLED : FinAp.STATUS_PARTIAL);
        apMapper.updateById(update);
    }

    /** 是否存在计划已生成的应收单（幂等判定）。 */
    public boolean existsArByPlan(String companyCode, Long planId) {
        return arMapper.selectCount(new LambdaQueryWrapper<FinAr>()
                .eq(FinAr::getCompanyCode, companyCode)
                .eq(FinAr::getPlanId, planId)) > 0;
    }

    /** 是否存在计划已生成的应付单（幂等判定）。 */
    public boolean existsApByPlan(String companyCode, Long planId) {
        return apMapper.selectCount(new LambdaQueryWrapper<FinAp>()
                .eq(FinAp::getCompanyCode, companyCode)
                .eq(FinAp::getPlanId, planId)) > 0;
    }

    /** 应收分页（公司隔离，status/关键字过滤）。 */
    public PageResult<FinAr> pageAr(String companyCode, String status, String keyword,
                                    long page, long size) {
        Page<FinAr> p = arMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FinAr>()
                        .eq(FinAr::getCompanyCode, companyCode)
                        .eq(status != null && !status.isBlank(), FinAr::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), FinAr::getCustomerName, keyword)
                        .orderByDesc(FinAr::getDueDate));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 应付分页（公司隔离，status/关键字过滤）。 */
    public PageResult<FinAp> pageAp(String companyCode, String status, String keyword,
                                    long page, long size) {
        Page<FinAp> p = apMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FinAp>()
                        .eq(FinAp::getCompanyCode, companyCode)
                        .eq(status != null && !status.isBlank(), FinAp::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), FinAp::getSupplierName, keyword)
                        .orderByDesc(FinAp::getDueDate));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 应收/应付账龄（F7，docs 4.7）：按到期日分档 未到期/0-30/31-60/61-90/90+，余额=未收付部分。 */
    public AgingVO aging(String companyCode, String type, LocalDate asOf) {
        AgingVO vo = new AgingVO();
        vo.setType(type);
        vo.setAsOf(asOf);
        boolean isAr = "AR".equalsIgnoreCase(type);
        java.util.Map<String, long[]> counts = new java.util.HashMap<>();
        java.util.Map<String, BigDecimal> sums = new java.util.HashMap<>();
        for (String bucket : java.util.List.of(AgingVO.NOT_DUE, AgingVO.D0_30,
                AgingVO.D31_60, AgingVO.D61_90, AgingVO.D90_PLUS)) {
            counts.put(bucket, new long[]{0});
            sums.put(bucket, BigDecimal.ZERO);
        }
        if (isAr) {
            List<FinAr> list = arMapper.selectList(new LambdaQueryWrapper<FinAr>()
                    .eq(FinAr::getCompanyCode, companyCode)
                    .ne(FinAr::getStatus, FinAr.STATUS_SETTLED)
                    .orderByAsc(FinAr::getDueDate));
            for (FinAr ar : list) {
                BigDecimal balance = ar.getAmount().subtract(ar.getReceivedAmount());
                String bucket = bucketOf(ar.getDueDate(), asOf);
                counts.get(bucket)[0]++;
                sums.put(bucket, sums.get(bucket).add(balance));
                vo.getDetail().add(new AgingVO.DetailItem(ar.getArNo(), ar.getCustomerName(),
                        ar.getDueDate(), ar.getAmount(), ar.getReceivedAmount(), balance,
                        daysOf(ar.getDueDate(), asOf), bucket));
            }
        } else {
            List<FinAp> list = apMapper.selectList(new LambdaQueryWrapper<FinAp>()
                    .eq(FinAp::getCompanyCode, companyCode)
                    .ne(FinAp::getStatus, FinAp.STATUS_SETTLED)
                    .orderByAsc(FinAp::getDueDate));
            for (FinAp ap : list) {
                BigDecimal balance = ap.getAmount().subtract(ap.getPaidAmount());
                String bucket = bucketOf(ap.getDueDate(), asOf);
                counts.get(bucket)[0]++;
                sums.put(bucket, sums.get(bucket).add(balance));
                vo.getDetail().add(new AgingVO.DetailItem(ap.getApNo(), ap.getSupplierName(),
                        ap.getDueDate(), ap.getAmount(), ap.getPaidAmount(), balance,
                        daysOf(ap.getDueDate(), asOf), bucket));
            }
        }
        for (String bucket : java.util.List.of(AgingVO.NOT_DUE, AgingVO.D0_30,
                AgingVO.D31_60, AgingVO.D61_90, AgingVO.D90_PLUS)) {
            vo.getSummary().add(new AgingVO.SummaryItem(bucket, counts.get(bucket)[0], sums.get(bucket)));
        }
        return vo;
    }

    // ==================== 内部实现 ====================

    /** 到期日与基准日天数差（到期日当天=0，未到期为负）。 */
    private long daysOf(LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(dueDate, asOf);
    }

    private String bucketOf(LocalDate dueDate, LocalDate asOf) {
        long days = daysOf(dueDate, asOf);
        if (days < 0) {
            return AgingVO.NOT_DUE;
        }
        if (days <= 30) {
            return AgingVO.D0_30;
        }
        if (days <= 60) {
            return AgingVO.D31_60;
        }
        if (days <= 90) {
            return AgingVO.D61_90;
        }
        return AgingVO.D90_PLUS;
    }


    private String nextArNo(String companyCode) {
        return nextNo(companyCode, "AR-", arMapper::selectLatestArNo);
    }

    private String nextApNo(String companyCode) {
        return nextNo(companyCode, "AP-", apMapper::selectLatestApNo);
    }

    private String nextNo(String companyCode, String prefix,
                          java.util.function.BiFunction<String, String, String> selector) {
        String key = prefix + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String latest = selector.apply(companyCode, key + "%");
        int seq = 1;
        if (latest != null) {
            seq = Integer.parseInt(latest.substring(latest.lastIndexOf('-') + 1)) + 1;
        }
        return key + String.format("%04d", seq);
    }
}
