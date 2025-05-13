package com.nan.config;

import com.nan.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 检查是否是共享链接请求，如果是则直接放行
            String requestURI = request.getRequestURI();
            if (requestURI != null && (requestURI.startsWith("/share/") || requestURI.equals("/share"))) {
                log.debug("共享链接请求，跳过JWT认证: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }
            
            // 尝试从请求头获取token
            final String authorizationHeader = request.getHeader("Authorization");
            String jwt = null;
            
            // 从请求头获取token
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
            } 
            // 如果请求头中没有token，尝试从URL参数中获取（用于EventSource连接）
            else {
                jwt = request.getParameter("token");
            }
            
            // 如果没有找到token或token格式错误，直接跳过处理
            if (jwt == null || jwt.trim().isEmpty() || jwt.chars().filter(ch -> ch == '.').count() != 2) {
                filterChain.doFilter(request, response);
                return;
            }
            
            try {
                // 提取用户名
                String username = jwtUtil.extractUsername(jwt);
                
                // 如果成功提取用户名且当前SecurityContext未设置认证信息
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    
                    // 验证令牌
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            } catch (Exception e) {
                // JWT解析错误，记录低级别日志
                if (log.isDebugEnabled()) {
                    log.debug("JWT令牌验证失败: {}", e.getMessage());
                }
                // 不处理，继续过滤器链
            }
        } catch (Exception e) {
            log.error("JWT认证过滤处理异常: {}", e.getMessage());
            // 不向上层抛出异常，允许请求继续
        }

        filterChain.doFilter(request, response);
    }
} 