package com.finance.system;

import com.finance.framework.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 操作日志集成测试（S4，docs 5.5）：@OperLog 切面真实落库 + 敏感脱敏 + 查询分页 + 失败留痕。
 *
 * <p>验证路径：MockMvc 调用「重置密码」（带 @OperLog，参数含 newPassword）→
 * 断言 sys_oper_log：title/operName/status/cost_time/operUrl 正确，密码已脱敏为 ******。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperLogAspectTest {

    private static final long ADMIN_ID = 1001L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String originalPasswordHash;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM sys_oper_log");
        // 备份 admin 密码哈希，测试结束恢复（避免污染 LoginLogWriteTest 的 admin/admin123）
        originalPasswordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE id = ?", String.class, ADMIN_ID);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("UPDATE sys_user SET password = ? WHERE id = ?",
                originalPasswordHash, ADMIN_ID);
        jdbcTemplate.update("DELETE FROM sys_oper_log");
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        LoginUser lu = new LoginUser(ADMIN_ID, "admin", "C001",
                List.of("admin"),
                List.of("system:user:reset-pwd", "system:log:list"));
        return new UsernamePasswordAuthenticationToken(lu, null,
                List.of(new SimpleGrantedAuthority("system:user:reset-pwd")));
    }

    @Test
    void resetPassword_writesOperLog_withMaskedParam() throws Exception {
        mockMvc.perform(put("/system/user/{id}/reset-pwd", ADMIN_ID)
                        .with(authentication(adminAuth()))
                        .contentType("application/json")
                        .content("{\"password\":\"OperLog@123\"}"))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT title, business_type, oper_name, status, cost_time, oper_url, " +
                        "request_method, oper_param FROM sys_oper_log ORDER BY oper_time DESC LIMIT 1");
        assertThat(row.get("title")).isEqualTo("用户重置密码");
        assertThat(row.get("oper_name")).isEqualTo("admin");
        assertThat(((Number) row.get("status")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("cost_time")).longValue()).isGreaterThanOrEqualTo(0);
        assertThat(row.get("oper_url")).isEqualTo("/system/user/1001/reset-pwd");
        assertThat(row.get("request_method")).isEqualTo("PUT");
        String param = String.valueOf(row.get("oper_param"));
        assertThat(param).contains("******");
        assertThat(param).doesNotContain("OperLog@123");
    }

    @Test
    void resetPassword_failure_writesStatus0WithErrorMsg() throws Exception {
        // 业务异常走统一 Result 包装（HTTP 200 + code!=200），日志必须记 status=0 + error_msg
        mockMvc.perform(put("/system/user/{id}/reset-pwd", 999999L)
                        .with(authentication(adminAuth()))
                        .contentType("application/json")
                        .content("{\"password\":\"OperLog@123\"}"))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, error_msg FROM sys_oper_log ORDER BY oper_time DESC LIMIT 1");
        assertThat(((Number) row.get("status")).intValue()).isZero();
        assertThat(String.valueOf(row.get("error_msg"))).isNotBlank();
    }

    @Test
    void operLogPage_queryByOperName_returnsRecords() throws Exception {
        // 先制造一条日志
        mockMvc.perform(put("/system/user/{id}/reset-pwd", ADMIN_ID)
                        .with(authentication(adminAuth()))
                        .contentType("application/json")
                        .content("{\"password\":\"OperLog@123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/system/oper-log").param("operName", "admin")
                        .param("page", "1").param("size", "10")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }
}
