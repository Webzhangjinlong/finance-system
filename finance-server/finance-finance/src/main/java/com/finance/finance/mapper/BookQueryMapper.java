package com.finance.finance.mapper;

import com.finance.finance.dto.BookRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 账簿查询专用 Mapper（F3）：总账/明细账/日记账聚合 SQL。
 *
 * <p>取数口径：已过账凭证（BOOKED + REVERSED，红冲借贷互换语义正确计入），
 * 按公司隔离；总账 LEFT JOIN 期初余额表（fin_balance_initial）。</p>
 */
@Mapper
public interface BookQueryMapper {

    /** 总账：按科目汇总期初 + 本期发生（期间内已过账凭证分录）。 */
    @Select("""
            SELECT e.subject_id           AS subjectId,
                   s.subject_code         AS subjectCode,
                   s.subject_name         AS subjectName,
                   s.subject_type         AS subjectType,
                   s.direction            AS direction,
                   COALESCE(bi.initial_debit, 0)  AS initialDebit,
                   COALESCE(bi.initial_credit, 0) AS initialCredit,
                   COALESCE(SUM(e.debit_amount), 0)  AS periodDebit,
                   COALESCE(SUM(e.credit_amount), 0) AS periodCredit
            FROM fin_voucher_entry e
                     JOIN fin_voucher v ON v.id = e.voucher_id AND v.company_code = e.company_code
                     JOIN fin_subject s ON s.id = e.subject_id
                     LEFT JOIN fin_balance_initial bi
                               ON bi.company_code = e.company_code
                                  AND bi.period_year = #{year} AND bi.period_month = #{month}
                                  AND bi.subject_id = e.subject_id
            WHERE e.company_code = #{companyCode}
              AND v.period_year = #{year} AND v.period_month = #{month}
              AND v.voucher_status IN ('BOOKED', 'REVERSED')
              AND v.deleted = 0 AND e.deleted = 0 AND s.deleted = 0
              AND (#{subjectId, jdbcType=BIGINT} IS NULL OR e.subject_id = #{subjectId, jdbcType=BIGINT})
            GROUP BY e.subject_id, s.subject_code, s.subject_name, s.subject_type, s.direction,
                     bi.initial_debit, bi.initial_credit
            ORDER BY s.subject_code
            """)
    List<BookRow> selectLedger(@Param("companyCode") String companyCode,
                               @Param("year") int year,
                               @Param("month") int month,
                               @Param("subjectId") Long subjectId);

    /** 明细账：期间内逐笔分录（凭证号/日期/摘要/借/贷）。 */
    @Select("""
            SELECT e.subject_id          AS subjectId,
                   s.subject_code        AS subjectCode,
                   s.subject_name        AS subjectName,
                   s.subject_type        AS subjectType,
                   s.direction           AS direction,
                   v.voucher_no          AS voucherNo,
                   TO_CHAR(v.voucher_date, 'YYYY-MM-DD') AS voucherDate,
                   e.summary             AS summary,
                   e.debit_amount        AS periodDebit,
                   e.credit_amount       AS periodCredit
            FROM fin_voucher_entry e
                     JOIN fin_voucher v ON v.id = e.voucher_id AND v.company_code = e.company_code
                     JOIN fin_subject s ON s.id = e.subject_id
            WHERE e.company_code = #{companyCode}
              AND v.period_year = #{year} AND v.period_month = #{month}
              AND v.voucher_status IN ('BOOKED', 'REVERSED')
              AND v.deleted = 0 AND e.deleted = 0
              AND (#{subjectId, jdbcType=BIGINT} IS NULL OR e.subject_id = #{subjectId, jdbcType=BIGINT})
            ORDER BY v.voucher_date, v.voucher_no, e.id
            """)
    List<BookRow> selectDetail(@Param("companyCode") String companyCode,
                               @Param("year") int year,
                               @Param("month") int month,
                               @Param("subjectId") Long subjectId);

    /** 日记账：按日期 + 科目逐日汇总（现金/银行日记账）。 */
    @Select("""
            SELECT e.subject_id   AS subjectId,
                   s.subject_code AS subjectCode,
                   s.subject_name AS subjectName,
                   s.direction    AS direction,
                   TO_CHAR(v.voucher_date, 'YYYY-MM-DD') AS voucherDate,
                   COALESCE(SUM(e.debit_amount), 0)  AS periodDebit,
                   COALESCE(SUM(e.credit_amount), 0) AS periodCredit
            FROM fin_voucher_entry e
                     JOIN fin_voucher v ON v.id = e.voucher_id AND v.company_code = e.company_code
                     JOIN fin_subject s ON s.id = e.subject_id
            WHERE e.company_code = #{companyCode}
              AND v.period_year = #{year} AND v.period_month = #{month}
              AND v.voucher_status IN ('BOOKED', 'REVERSED')
              AND v.deleted = 0 AND e.deleted = 0
              AND (#{subjectId, jdbcType=BIGINT} IS NULL OR e.subject_id = #{subjectId, jdbcType=BIGINT})
            GROUP BY e.subject_id, s.subject_code, s.subject_name, s.direction, v.voucher_date
            ORDER BY v.voucher_date, s.subject_code
            """)
    List<BookRow> selectJournal(@Param("companyCode") String companyCode,
                                @Param("year") int year,
                                @Param("month") int month,
                                @Param("subjectId") Long subjectId);
}
