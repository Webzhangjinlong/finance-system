package com.finance.system.domain;

import com.finance.framework.security.LoginUser;

import java.util.List;

/**
 * 登录成功返回体：JWT + 用户信息 + 角色 + 权限码（docs 3.1）。
 */
public class LoginUserVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String companyCode;
    private List<String> roles;
    private List<String> permissions;

    public static LoginUserVO from(LoginUser user, String token, String nickname) {
        LoginUserVO vo = new LoginUserVO();
        vo.token = token;
        vo.userId = user.getUserId();
        vo.username = user.getUsername();
        vo.nickname = nickname;
        vo.companyCode = user.getCompanyCode();
        vo.roles = user.getRoles();
        vo.permissions = user.getPermissions();
        return vo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
