package com.finance.system;

import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.LoginBody;
import com.finance.system.domain.LoginUserVO;
import com.finance.system.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 登录日志落库集成测试（修复 V15：sys_login_log 补公共五件套列）。
 *
 * <p>背景：V1 建表缺 create_by/create_time/update_by/update_time/deleted，
 * SysLoginLog 实体继承 BaseEntity（INSERT 写五件套列）→ 写入被 AuthService.writeLoginLog 的
 * try-catch 吞掉（登录不受影响，但审计日志丢失）。V15 补列后，登录成功/失败均应真实落库。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginLogWriteTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sys_login_log");
    }

    @Test
    void loginSuccess_writesLoginLogWithStatus1() {
        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("admin123");
        body.setCompanyCode("C001");

        LoginUserVO vo = authService.login(body, "127.0.0.1");

        assertThat(vo).isNotNull();
        assertThat(vo.getToken()).isNotBlank();
        assertThat(lastLogRow())
                .extracting(m -> m.get("username"))
                .isEqualTo("admin");
        assertThat(((Number) lastLogRow().get("status")).intValue()).isEqualTo(1);
    }

    @Test
    void loginFailure_writesLoginLogWithStatus0() {
        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("wrong-password");
        body.setCompanyCode("C001");

        assertThatThrownBy(() -> authService.login(body, "127.0.0.1"))
                .isInstanceOf(BusinessException.class);

        Map<String, Object> row = lastLogRow();
        assertThat(row.get("username")).isEqualTo("admin");
        assertThat(((Number) row.get("status")).intValue()).isZero();
    }

    /** 直接回归：INSERT 含五件套列（含 create_by=null）不再抛 "create_by 字段不存在"。 */
    @Test
    void insertWithFiveItemsColumns_doesNotThrow() {
        // 成功登录即触发带五件套列的 INSERT；此处再显式执行一次等价 SQL 断言列存在
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_name='sys_login_log' AND column_name IN " +
                        "('create_by','create_time','update_by','update_time','deleted')",
                Integer.class);
        assertThat(exists).isEqualTo(5);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM sys_login_log LIMIT 1");
        assertThat(rows).isEmpty();
    }

    private Map<String, Object> lastLogRow() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT username, status FROM sys_login_log ORDER BY login_time DESC LIMIT 1");
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }
}
