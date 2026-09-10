package com.finance.framework.security;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户上下文（SecurityContext principal）。
 *
 * <p>由 JwtAuthenticationFilter 从 token 载入，业务侧通过 {@link SecurityUtils} 读取，
 * 硬约束 2（公司隔离）依赖其中的 companyCode。</p>
 */
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String companyCode;
    private List<String> roles;
    private List<String> permissions;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String companyCode,
                     List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.companyCode = companyCode;
        this.roles = roles;
        this.permissions = permissions;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
