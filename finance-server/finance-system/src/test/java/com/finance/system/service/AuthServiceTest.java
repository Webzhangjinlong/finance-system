package com.finance.system.service;

import com.finance.common.core.exception.BusinessException;
import com.finance.framework.security.JwtUtils;
import com.finance.framework.security.LoginFailCounter;
import com.finance.system.domain.LoginBody;
import com.finance.system.domain.LoginUserVO;
import com.finance.system.domain.SysLoginLog;
import com.finance.system.domain.SysUser;
import com.finance.system.mapper.SysLoginLogMapper;
import com.finance.system.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务测试（S1）：BCrypt 校验、失败锁定、JWT 签发（硬约束 21）。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysLoginLogMapper loginLogMapper;
    @Mock
    private LoginFailCounter loginFailCounter;

    private AuthService authService;
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils("finance-system-jwt-secret-key-please-change-0123456789", 7200);
        authService = new AuthService(userMapper, loginLogMapper,
                new BCryptPasswordEncoder(), jwtUtils, loginFailCounter);
    }

    private SysUser adminUser() {
        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername("admin");
        user.setPassword(new BCryptPasswordEncoder().encode("admin123"));
        user.setNickname("系统管理员");
        user.setStatus("ACTIVE");
        return user;
    }

    @Test
    void loginSuccess_returnsTokenWithClaims() {
        SysUser user = adminUser();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectRoleKeysByUserId(1001L)).thenReturn(List.of("admin"));
        when(userMapper.selectPermsByUserId(1001L)).thenReturn(List.of("finance:voucher:add"));
        when(loginFailCounter.isLocked("admin")).thenReturn(false);

        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("admin123");
        body.setCompanyCode("DEMO");

        LoginUserVO vo = authService.login(body, "127.0.0.1");

        assertThat(vo.getToken()).isNotBlank();
        assertThat(vo.getUsername()).isEqualTo("admin");
        assertThat(vo.getCompanyCode()).isEqualTo("DEMO");
        assertThat(vo.getRoles()).contains("admin");
        assertThat(vo.getPermissions()).contains("finance:voucher:add");

        // token 可解析且包含上下文
        Claims claims = jwtUtils.parseToken(vo.getToken());
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get(JwtUtils.CLAIM_COMPANY)).isEqualTo("DEMO");

        // 成功后清除失败计数 + 记录登录日志
        verify(loginFailCounter).clearFailure("admin");
        verify(loginLogMapper).insert(any(SysLoginLog.class));
    }

    @Test
    void loginWithWrongPassword_recordsFailureAndRejects() {
        SysUser user = adminUser();
        when(userMapper.selectOne(any())).thenReturn(user);
        when(loginFailCounter.isLocked("admin")).thenReturn(false);
        when(loginFailCounter.recordFailure("admin")).thenReturn(1L);

        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("wrong-password");
        body.setCompanyCode("DEMO");

        assertThatThrownBy(() -> authService.login(body, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(loginFailCounter).recordFailure("admin");
        verify(loginFailCounter, never()).clearFailure("admin");
    }

    @Test
    void loginWhenLocked_isRejected() {
        when(loginFailCounter.isLocked("admin")).thenReturn(true);
        when(loginFailCounter.lockRemainingSeconds("admin")).thenReturn(300L);

        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("admin123");

        assertThatThrownBy(() -> authService.login(body, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("锁定");

        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void loginDisabledUser_isRejected() {
        SysUser user = adminUser();
        user.setStatus("DISABLED");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(loginFailCounter.isLocked("admin")).thenReturn(false);

        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("admin123");

        assertThatThrownBy(() -> authService.login(body, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用");
    }

    @Test
    void loginUnknownUser_recordsFailure() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(loginFailCounter.isLocked("ghost")).thenReturn(false);
        when(loginFailCounter.recordFailure("ghost")).thenReturn(1L);

        LoginBody body = new LoginBody();
        body.setUsername("ghost");
        body.setPassword("anything");

        assertThatThrownBy(() -> authService.login(body, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(loginFailCounter).recordFailure("ghost");
    }
}
