package com.finance.framework.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer &lt;token&gt;，校验通过后
 * 将 LoginUser（含权限码）载入 SecurityContext，@PreAuthorize 据此判定（硬约束 15/21）。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtUtils.parseToken(token);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Object uidObj = claims.get(JwtUtils.CLAIM_USER_ID);
                String companyCode = claims.get(JwtUtils.CLAIM_COMPANY, String.class);
                List<String> roles = claims.get(JwtUtils.CLAIM_ROLES, List.class);
                List<String> permissions = claims.get(JwtUtils.CLAIM_PERMS, List.class);
                LoginUser loginUser = new LoginUser(
                        uidObj instanceof Number n ? n.longValue() : null,
                        claims.getSubject(),
                        companyCode,
                        roles == null ? new ArrayList<>() : new ArrayList<>(roles),
                        permissions == null ? new ArrayList<>() : new ArrayList<>(permissions));
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("AUTHENTICATED"));
                for (String perm : loginUser.getPermissions()) {
                    if (StringUtils.hasText(perm)) {
                        authorities.add(new SimpleGrantedAuthority(perm));
                    }
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
