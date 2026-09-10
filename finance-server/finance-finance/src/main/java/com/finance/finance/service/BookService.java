package com.finance.finance.service;

import com.alibaba.excel.EasyExcel;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.dto.BookRow;
import com.finance.finance.mapper.BookQueryMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 账簿查询服务（F3，docs 4.3）：总账 / 明细账 / 日记账。
 *
 * <p>取数口径：已过账凭证（BOOKED + REVERSED，红冲借贷互换语义正确计入）。
 * 总账 = 期初（fin_balance_initial）+ 本期发生 → 按科目方向归集期末余额；
 * 明细账逐笔列示并累计余额；日记账按日汇总。支持 Excel 导出（EasyExcel）。</p>
 */
@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookQueryMapper bookQueryMapper;

    public BookService(BookQueryMapper bookQueryMapper) {
        this.bookQueryMapper = bookQueryMapper;
    }

    /** 总账：科目期初 / 本期借 / 本期贷 / 期末余额（方向感知）。 */
    public List<BookRow> ledger(String companyCode, int year, int month, Long subjectId) {
        List<BookRow> rows = bookQueryMapper.selectLedger(companyCode, year, month, subjectId);
        for (BookRow row : rows) {
            calculateEnding(row);
        }
        return rows;
    }

    /** 明细账：逐笔分录 + 运行余额（按科目方向归集）。 */
    public List<BookRow> detail(String companyCode, int year, int month, Long subjectId) {
        List<BookRow> rows = bookQueryMapper.selectDetail(companyCode, year, month, subjectId);
        attachRunningBalance(rows);
        return rows;
    }

    /** 日记账：按日汇总 + 日末累计余额。 */
    public List<BookRow> journal(String companyCode, int year, int month, Long subjectId) {
        List<BookRow> rows = bookQueryMapper.selectJournal(companyCode, year, month, subjectId);
        attachRunningBalance(rows);
        return rows;
    }

    /** 按账簿类型导出 Excel（response 直接写出 xlsx）。 */
    public void export(String companyCode, String type, int year, int month, Long subjectId,
                       HttpServletResponse response) {
        List<BookRow> rows = switch (type) {
            case "ledger" -> ledger(companyCode, year, month, subjectId);
            case "detail" -> detail(companyCode, year, month, subjectId);
            case "journal" -> journal(companyCode, year, month, subjectId);
            default -> throw new BusinessException("不支持的账簿类型：" + type);
        };
        try {
            String fileName = URLEncoder.encode(type + "-" + year + "-" + month + ".xlsx",
                    StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=UTF-8''" + fileName);
            EasyExcel.write(response.getOutputStream(), BookRow.class).sheet(type).doWrite(rows);
        } catch (IOException e) {
            log.error("账簿 Excel 导出失败：type={} period={}-{}", type, year, month, e);
            throw new BusinessException("Excel 导出失败", e);
        }
    }

    // ==================== 内部实现 ====================

    /** 按科目方向归集期末余额：净额先归一为"正向余额"，再按方向落入借/贷列。 */
    private void calculateEnding(BookRow row) {
        BigDecimal initial = nz(row.getInitialDebit()).subtract(nz(row.getInitialCredit()));
        BigDecimal period = nz(row.getPeriodDebit()).subtract(nz(row.getPeriodCredit()));
        BigDecimal balance = initial.add(period);
        if ("CREDIT".equals(row.getDirection())) {
            balance = balance.negate();
        }
        assignByDirection(row, balance);
    }

    /** 运行余额：从期初 0 起逐行累计（借加贷减），按科目方向归集到借/贷列。 */
    private void attachRunningBalance(List<BookRow> rows) {
        BigDecimal running = BigDecimal.ZERO;
        for (BookRow row : rows) {
            running = running.add(nz(row.getPeriodDebit())).subtract(nz(row.getPeriodCredit()));
            BigDecimal display = running;
            if ("CREDIT".equals(row.getDirection())) {
                display = running.negate();
            }
            assignByDirection(row, display);
        }
    }

    /** 正向余额 ≥0 落入科目方向列；负数（异常反向）落入另一列并取绝对值。 */
    private void assignByDirection(BookRow row, BigDecimal balance) {
        boolean creditSubject = "CREDIT".equals(row.getDirection());
        if (balance.signum() >= 0) {
            if (creditSubject) {
                row.setEndingCredit(balance);
                row.setEndingDebit(BigDecimal.ZERO);
            } else {
                row.setEndingDebit(balance);
                row.setEndingCredit(BigDecimal.ZERO);
            }
        } else {
            BigDecimal abs = balance.negate();
            if (creditSubject) {
                row.setEndingDebit(abs);
                row.setEndingCredit(BigDecimal.ZERO);
            } else {
                row.setEndingCredit(abs);
                row.setEndingDebit(BigDecimal.ZERO);
            }
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
