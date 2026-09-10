package com.finance.finance;

import com.finance.finance.domain.FinAp;
import com.finance.finance.domain.FinAr;
import com.finance.finance.domain.vo.AgingVO;
import com.finance.finance.mapper.FinApMapper;
import com.finance.finance.mapper.FinArMapper;
import com.finance.finance.service.ArApService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应收/应付账龄集成测试（F7，docs 4.7）：分档规则、余额口径、已结清排除。
 */
@SpringBootTest
@ActiveProfiles("test")
class AgingTest {

    @Autowired
    private ArApService arApService;
    @Autowired
    private FinArMapper arMapper;
    @Autowired
    private FinApMapper apMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("DELETE FROM fin_ar");
        jdbcTemplate.execute("DELETE FROM fin_ap");
    }

    private FinAr ar(String arNo, String customer, BigDecimal amount, BigDecimal received,
                     String status, LocalDate dueDate) {
        FinAr ar = new FinAr();
        ar.setCompanyCode("DEMO");
        ar.setArNo(arNo);
        ar.setCustomerName(customer);
        ar.setAmount(amount);
        ar.setReceivedAmount(received);
        ar.setStatus(status);
        ar.setDueDate(dueDate);
        arMapper.insert(ar);
        return ar;
    }

    private FinAp ap(String apNo, String supplier, BigDecimal amount, BigDecimal paid,
                     String status, LocalDate dueDate) {
        FinAp ap = new FinAp();
        ap.setCompanyCode("DEMO");
        ap.setApNo(apNo);
        ap.setSupplierName(supplier);
        ap.setAmount(amount);
        ap.setPaidAmount(paid);
        ap.setStatus(status);
        ap.setDueDate(dueDate);
        apMapper.insert(ap);
        return ap;
    }

    @Test
    void arAging_bucketsByDueDate() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);
        ar("AR001", "客户甲", new BigDecimal("1000"), BigDecimal.ZERO, FinAr.STATUS_OPEN, asOf.plusDays(5));
        ar("AR002", "客户乙", new BigDecimal("2000"), new BigDecimal("500"), FinAr.STATUS_PARTIAL, asOf.minusDays(10));
        ar("AR003", "客户丙", new BigDecimal("3000"), BigDecimal.ZERO, FinAr.STATUS_OPEN, asOf.minusDays(45));
        ar("AR004", "客户丁", new BigDecimal("4000"), BigDecimal.ZERO, FinAr.STATUS_OPEN, asOf.minusDays(100));
        ar("AR005", "客户戊", new BigDecimal("5000"), new BigDecimal("5000"), FinAr.STATUS_SETTLED, asOf.minusDays(60));

        AgingVO vo = arApService.aging("DEMO", "AR", asOf);

        assertThat(vo.getType()).isEqualTo("AR");
        assertThat(vo.getAsOf()).isEqualTo(asOf);
        // 未结清 4 笔，SETTLED 排除
        assertThat(vo.getDetail()).hasSize(4);
        assertThat(summaryOf(vo, AgingVO.NOT_DUE).getAmount()).isEqualByComparingTo("1000");
        assertThat(summaryOf(vo, AgingVO.D0_30).getAmount()).isEqualByComparingTo("1500"); // 2000-500
        assertThat(summaryOf(vo, AgingVO.D31_60).getAmount()).isEqualByComparingTo("3000");
        assertThat(summaryOf(vo, AgingVO.D90_PLUS).getAmount()).isEqualByComparingTo("4000");
        assertThat(summaryOf(vo, AgingVO.D61_90).getCount()).isZero();
        assertThat(vo.getDetail()).anySatisfy(d -> {
            assertThat(d.getDocNo()).isEqualTo("AR002");
            assertThat(d.getDays()).isEqualTo(10);
            assertThat(d.getBucket()).isEqualTo(AgingVO.D0_30);
            assertThat(d.getBalance()).isEqualByComparingTo("1500");
        });
    }

    @Test
    void apAging_partialAndBoundary() {
        LocalDate asOf = LocalDate.of(2026, 9, 10);
        ap("AP001", "供应商甲", new BigDecimal("1000"), BigDecimal.ZERO, FinAp.STATUS_OPEN, asOf.minusDays(30));
        ap("AP002", "供应商乙", new BigDecimal("2000"), new BigDecimal("2000"), FinAp.STATUS_SETTLED, asOf.minusDays(3));
        ap("AP003", "供应商丙", new BigDecimal("3000"), new BigDecimal("1000"), FinAp.STATUS_PARTIAL, asOf.minusDays(61));

        AgingVO vo = arApService.aging("DEMO", "AP", asOf);

        // 30 天整 → D0_30；61 天 → D61_90；SETTLED 排除
        assertThat(vo.getDetail()).hasSize(2);
        assertThat(summaryOf(vo, AgingVO.D0_30).getCount()).isEqualTo(1);
        assertThat(summaryOf(vo, AgingVO.D61_90).getCount()).isEqualTo(1);
        assertThat(summaryOf(vo, AgingVO.D61_90).getAmount()).isEqualByComparingTo("2000");
        assertThat(vo.getDetail()).anySatisfy(d -> {
            assertThat(d.getDocNo()).isEqualTo("AP003");
            assertThat(d.getBucket()).isEqualTo(AgingVO.D61_90);
        });
    }

    private AgingVO.SummaryItem summaryOf(AgingVO vo, String bucket) {
        return vo.getSummary().stream()
                .filter(s -> bucket.equals(s.getBucket()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("bucket missing: " + bucket));
    }
}
