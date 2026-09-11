package com.finance.finance.service;

import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinSubject;
import com.finance.finance.domain.vo.ReportRow;
import com.finance.finance.domain.vo.TrialBalanceRow;
import com.finance.finance.domain.vo.TrialBalanceVO;
import com.finance.finance.mapper.FinSubjectMapper;
import com.finance.finance.mapper.ReportQueryMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务分析服务（F9）：科目余额表（试算平衡）+ 费用月度趋势。
 *
 * <p>口径（与 R38 报表一致）：仅 BOOKED 凭证；红字冲销自然并入，被冲销原凭证 REVERSED 排除；
 * 期初 = 截止上期累计，本期 = 期间发生，期末 = 期初 + 本期；按科目 direction 归位借贷栏；
 * 试算平衡 = 期初/本期/期末三对借合计 == 贷合计（机器断言，失衡即异常）。</p>
 */
@Service
public class AnalysisService {

    private final ReportQueryMapper reportQueryMapper;
    private final FinSubjectMapper subjectMapper;

    public AnalysisService(ReportQueryMapper reportQueryMapper, FinSubjectMapper subjectMapper) {
        this.reportQueryMapper = reportQueryMapper;
        this.subjectMapper = subjectMapper;
    }

    /** 科目余额表（试算平衡）：期初/本期/期末 借贷双栏 + 合计 + 平衡断言。 */
    public TrialBalanceVO trialBalance(String companyCode, int periodYear, int periodMonth) {
        if (periodYear <= 0 || periodMonth < 1 || periodMonth > 12) {
            throw new BusinessException("期间不合法（year>0，month 1-12）");
        }
        // 期初 = 截止上期累计；本期 = 期间发生
        Map<String, ReportRow> opening = index(reportQueryMapper.selectReportRowsUpTo(companyCode, periodYear, periodMonth));
        Map<String, ReportRow> period = index(reportQueryMapper.selectReportRows(companyCode, periodYear, periodMonth));

        // 全量科目补齐（无发生/无余额科目也展示）
        Map<String, FinSubject> subjects = new LinkedHashMap<>();
        for (FinSubject s : subjectMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinSubject>()
                        .eq(FinSubject::getCompanyCode, companyCode)
                        .orderByAsc(FinSubject::getSubjectCode))) {
            subjects.put(s.getSubjectCode(), s);
        }

        TrialBalanceVO vo = new TrialBalanceVO();
        vo.setPeriodYear(periodYear);
        vo.setPeriodMonth(periodMonth);
        List<TrialBalanceRow> rows = new ArrayList<>();
        BigDecimal od = BigDecimal.ZERO, oc = BigDecimal.ZERO;
        BigDecimal pd = BigDecimal.ZERO, pc = BigDecimal.ZERO;
        BigDecimal cd = BigDecimal.ZERO, cc = BigDecimal.ZERO;

        for (FinSubject s : subjects.values()) {
            TrialBalanceRow r = new TrialBalanceRow();
            r.setSubjectCode(s.getSubjectCode());
            r.setSubjectName(s.getSubjectName());
            r.setSubjectType(s.getSubjectType());
            r.setDirection(s.getDirection());
            BigDecimal opDebit = BigDecimal.ZERO, opCredit = BigDecimal.ZERO;
            ReportRow o = opening.get(s.getSubjectCode());
            if (o != null) {
                opDebit = nz(o.getPeriodDebit());
                opCredit = nz(o.getPeriodCredit());
            }
            BigDecimal peDebit = BigDecimal.ZERO, peCredit = BigDecimal.ZERO;
            ReportRow p = period.get(s.getSubjectCode());
            if (p != null) {
                peDebit = nz(p.getPeriodDebit());
                peCredit = nz(p.getPeriodCredit());
            }
            // 期末净额 = 期初净额 + 本期净额，按科目方向归位借贷栏
            BigDecimal net = opDebit.subtract(opCredit).add(peDebit).subtract(peCredit);
            BigDecimal clDebit = BigDecimal.ZERO, clCredit = BigDecimal.ZERO;
            if (FinSubject.DIR_DEBIT.equals(s.getDirection())) {
                // 借方方向科目（资产/成本/费用）：正净额记借方余额，负净额记贷方余额
                clDebit = net.signum() > 0 ? net : BigDecimal.ZERO;
                clCredit = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
            } else {
                // 贷方方向科目（负债/权益/收入）：负净额（贷方净额）记贷方余额，正净额记借方余额
                clCredit = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
                clDebit = net.signum() > 0 ? net : BigDecimal.ZERO;
            }
            r.setOpeningDebit(opDebit);
            r.setOpeningCredit(opCredit);
            r.setPeriodDebit(peDebit);
            r.setPeriodCredit(peCredit);
            r.setClosingDebit(clDebit);
            r.setClosingCredit(clCredit);
            r.setActive(opDebit.signum() != 0 || opCredit.signum() != 0 || peDebit.signum() != 0
                    || peCredit.signum() != 0 || clDebit.signum() != 0 || clCredit.signum() != 0);
            rows.add(r);
            od = od.add(opDebit); oc = oc.add(opCredit);
            pd = pd.add(peDebit); pc = pc.add(peCredit);
            cd = cd.add(clDebit); cc = cc.add(clCredit);
        }
        vo.setRows(rows);
        vo.setOpeningDebitTotal(od);
        vo.setOpeningCreditTotal(oc);
        vo.setPeriodDebitTotal(pd);
        vo.setPeriodCreditTotal(pc);
        vo.setClosingDebitTotal(cd);
        vo.setClosingCreditTotal(cc);
        vo.setBalanced(od.compareTo(oc) == 0 && pd.compareTo(pc) == 0 && cd.compareTo(cc) == 0);
        return vo;
    }

    /** 费用月度趋势：费用科目（PROFIT+DEBIT）按 科目×月 借方汇总（当年 1-12 月）。 */
    public List<ReportRow> expenseTrend(String companyCode, int periodYear) {
        if (periodYear <= 0) {
            throw new BusinessException("年份不合法");
        }
        return reportQueryMapper.selectExpenseTrend(companyCode, periodYear);
    }

    private Map<String, ReportRow> index(List<ReportRow> list) {
        Map<String, ReportRow> map = new HashMap<>();
        if (list != null) {
            for (ReportRow r : list) {
                map.put(r.getSubjectCode(), r);
            }
        }
        return map;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
