package com.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文加载 + Flyway 落库验证。
 *
 * <p>上下文加载成功即证明：Flyway 迁移（V1/V2/V3）在测试库执行成功、
 * Flowable 引擎被排除、MyBatis-Plus / Redis / Security 配置装配成功。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class FinanceApplicationContextTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        // 能走到这里即上下文装配成功
        assertThat(dataSource).isNotNull();
    }

    @Test
    void flywayMigratedToLatest() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT MAX(version) FROM flyway_schema_history WHERE success = true")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("3");
        }
    }

    @Test
    void seedDataPresent() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT count(*) FROM fin_company WHERE company_code = 'DEMO'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isEqualTo(1L);
        }
    }
}
