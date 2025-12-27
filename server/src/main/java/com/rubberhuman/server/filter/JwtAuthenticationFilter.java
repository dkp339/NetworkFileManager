package com.rubberhuman.server.filter;

import com.rubberhuman.server.util.JwtTokenUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = req.getHeader("Authorization");

        String username = null;
        String jwtToken = null;
        Claims claims = null;

        // 1. 检查 Token 格式
        // 如果没有 Bearer 头，说明可能是匿名访问，或者没有登录
        // 不需要打印 Warn 日志，保持控制台清爽
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                // 解析 Token (这步会同时校验签名和过期时间)
                claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
                username = claims.getSubject();
            } catch (ExpiredJwtException e) {
                // Token 过期：返回 401
                logger.warn("JWT Token 已过期: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Expired");
                return;
            } catch (SignatureException | MalformedJwtException e) {
                logger.error("JWT 签名无效或格式错误");
            } catch (Exception e) {
                logger.error("JWT 解析失败", e);
            }
        }

        // 2. 构建认证信息 (无状态模式：不查数据库)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 直接从 Claims 中读取权限列表
            // 注意：这需要与 generateToken 中的逻辑对应
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = null;
            if (roles != null) {
                authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // 构建 Authentication 对象
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username, // Principal
                    null,     // Credentials (JWT模式下通常为空)
                    authorities // Authorities
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

            // 设置到上下文
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        chain.doFilter(req, resp);
    }


}

