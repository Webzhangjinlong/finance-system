package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.core.exception.BusinessException;
import com.finance.framework.security.JwtUtils;
import com.finance.framework.security.LoginFailCounter;
import com.finance.framework.security.LoginUser;
import com.finance.framework.security.SecurityUtils;
import com.finance.system.domain.LoginBody;
import com.finance.system.domain.LoginUserVO;
import com.finance.system.domain.SysLoginLog;
import com.finance.system.domain.SysUser;
import com.finance.system.mapper.SysLoginLogMapper;
import com.finance.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 认证服务（S1，docs 3.1）：账号密码登录、登出、当前用户信息。
 *
 * <p>硬约束 21：BCrypt 校验、JWT 无状态 2h、失败 5 次锁定 15 分钟；
 * 硬约束 2：登录上下文携带 companyCode 供业务隔离。</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_COMPANY = "DEMO";

    private final SysUserMapper userMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final LoginFailCounter loginFailCounter;

    public AuthService(SysUserMapper userMapper,
                       SysLoginLogMapper loginLogMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       LoginFailCounter loginFailCounter) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.loginFailCounter = loginFailCounter;
    }

    @Transactional
    public LoginUserVO login(LoginBody body, String ip) {
        String username = body.getUsername().trim();
        String companyCode = body.getCompanyCode() == null || body.getCompanyCode().isBlank()
                ? DEFAULT_COMPANY : body.getCompanyCode().trim();

        // 1. 锁定检查（硬约束 21）
        if (loginFailCounter.isLocked(username)) {
            long remain = loginFailCounter.lockRemainingSeconds(username);
            writeLoginLog(username, ip, 0, "账号已锁定，请 " + remain + " 秒后再试");
            throw new BusinessException("账号已锁定，请 " + remain + " 秒后再试");
        }

        // 2. 查询用户
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(body.getPassword(),
                user.getPassword() == null ? "" : user.getPassword())) {
            long failed = loginFailCounter.recordFailure(username);
            writeLoginLog(username, ip, 0, "用户名或密码错误（第 " + failed + " 次）");
            throw new BusinessException("用户名或密码错误，连续失败 " + loginFailCounter.maxFailTimesHint()
                    + " 次将锁定 15 分钟");
        }

        // 3. 账号状态
        if (!"ACTIVE".equals(user.getStatus())) {
            writeLoginLog(username, ip, 0, "账号已停用");
            throw new BusinessException("账号已停用，请联系管理员");
        }

        // 4. 成功：清计数、加载角色权限、签发 JWT
        loginFailCounter.clearFailure(username);
        List<String> roles = userMapper.selectRoleKeysByUserId(user.getId());
        List<String> perms = userMapper.selectPermsByUserId(user.getId());
        LoginUser loginUser = new LoginUser(user.getId(), username, companyCode, roles, perms);
        String token = jwtUtils.createToken(user.getId(), username, companyCode, roles, perms);

        userMapper.updateLoginInfo(user.getId(), ip, OffsetDateTime.now());
        writeLoginLog(username, ip, 1, "登录成功");
        log.info("用户 [{}] 登录成功，账套 [{}]", username, companyCode);
        return LoginUserVO.from(loginUser, token, user.getNickname());
    }

    /** 登出：JWT 无状态，前端清理 token；服务端留审计日志。 */
    public void logout() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user != null) {
            writeLoginLog(user.getUsername(), null, 1, "登出");
            log.info("用户 [{}] 登出", user.getUsername());
        }
    }

    /** 当前用户信息（含权限码，供前端路由守卫）。 */
    public LoginUserVO profile() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return LoginUserVO.from(user, null, user.getUsername());
    }

    private void writeLoginLog(String username, String ip, int status, String msg) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setLoginIp(ip);
        loginLog.setStatus(status);
        loginLog.setMsg(msg);
        loginLog.setLoginTime(OffsetDateTime.now());
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.warn("写登录日志失败: {}", e.getMessage());
        }
    }
}
