package com.finance.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具：签发与解析无状态令牌（硬约束 21：JWT 无状态，默认 2h）。
 *
 * <p>密钥从配置注入（生产用环境变量覆盖），禁止硬编码进代码/提交。
 * token 内携带 uid/companyCode/roles/permissions，解析后还原 LoginUser，
 * 供 @PreAuthorize 权限码判定（权限变更需重新登录，一期无状态语义）。</p>
 */
@Component
public class JwtUtils {

    /** 用户 ID claim 名 */
    public static final String CLAIM_USER_ID = "uid";
    /** 公司编码 claim 名（多公司上下文） */
    public static final String CLAIM_COMPANY = "companyCode";
    /** 角色 claim 名 */
    public static final String CLAIM_ROLES = "roles";
    /** 权限码 claim 名 */
    public static final String CLAIM_PERMS = "perms";

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtils(
            @Value("${finance.jwt.secret:finance-system-jwt-secret-key-please-change-0123456789}") String secret,
            @Value("${finance.jwt.expire-seconds:7200}") long expireSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireSeconds * 1000L;
    }

    /** 签发 token：subject=用户名，携带用户 ID、公司编码、角色与权限码。 */
    public String createToken(Long userId, String username, String companyCode,
                              List<String> roles, List<String> permissions) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_COMPANY, companyCode)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMS, permissions)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /** 解析 token；非法/过期返回 null。 */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
