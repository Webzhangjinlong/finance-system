package com.finance.finance.mapper;

import com.finance.finance.domain.vo.ReportRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务报表取数（F5，docs 4.5）：已过账凭证（BOOKED）按科目汇总。
 *
 * <p>口径：红冲生成的红字凭证为 BOOKED 自然并入，被冲销原凭证 REVERSED 被排除；
 * periodYear/periodMonth 同时传入时按期间精确过滤，否则全量累计。</p>
 */
@Mapper
public interface ReportQueryMapper {

    @Select("""
            SELECT s.subject_code AS subjectCode,
                   s.subject_name AS subjectName,
                   s.subject_type AS subjectType,
                   s.direction     AS direction,
                   COALESCE(SUM(e.debit_amount), 0)  AS periodDebit,
                   COALESCE(SUM(e.credit_amount), 0) AS periodCredit
            FROM fin_voucher_entry e
            JOIN fin_subject s ON s.id = e.subject_id
            JOIN fin_voucher v ON v.id = e.voucher_id AND v.voucher_status = 'BOOKED'
            WHERE e.company_code = #{companyCode}
              AND (CAST(#{periodYear} AS INTEGER) IS NULL OR v.period_year = #{periodYear})
              AND (CAST(#{periodMonth} AS INTEGER) IS NULL OR v.period_month = #{periodMonth})
            GROUP BY s.subject_code, s.subject_name, s.subject_type, s.direction
            ORDER BY s.subject_code
            """)
    List<ReportRow> selectReportRows(@Param("companyCode") String companyCode,
                                     @Param("periodYear") Integer periodYear,
                                     @Param("periodMonth") Integer periodMonth);
}
