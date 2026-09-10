package com.finance.framework.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：业务代码统一从这里读取当前登录用户与公司编码。
 *
 * <p>未登录场景返回安全默认值（null），调用方自行决定是否放行。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户；未登录返回 null。 */
    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 当前用户 ID；未登录返回 null。 */
    public static Long getUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUserId() : null;
    }

    /** 当前用户名；未登录返回 null。 */
    public static String getUsername() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUsername() : null;
    }

    /** 当前公司编码（硬约束 2 隔离依据）；未登录返回 null。 */
    public static String getCompanyCode() {
        LoginUser user = getLoginUser();
        return user != null ? user.getCompanyCode() : null;
    }

    /** 当前用户是否拥有指定权限码（@PreAuthorize 之外的编程式校验）。 */
    public static boolean hasPermission(String permission) {
        LoginUser user = getLoginUser();
        if (user == null || user.getPermissions() == null) {
            return false;
        }
        return user.getPermissions().contains(permission);
    }
}
