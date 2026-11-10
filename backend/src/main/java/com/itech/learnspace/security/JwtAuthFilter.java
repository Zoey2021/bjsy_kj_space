package com.itech.learnspace.security;

import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.repository.SysUserRepository;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.util.JwtUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 鉴权过滤器：每个请求进来时检查是否携带有效 Token
 * 流程：从请求头取 Token → 解析 → 查到用户 → 放入 Security 上下文 → 放行
 * 仍使用弱口令/未完成强制改密的账号，只能访问改密与个人资料接口。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserRepository userRepository;
    private final AuthService authService;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         SysUserRepository userRepository,
                         @Lazy AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // Token 有效且当前还没登录 → 设置用户身份
        if (StringUtils.hasText(token) && jwtUtil.isTokenValid(token)) {
            String username = jwtUtil.getUsername(token);
            SysUser user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.getStatus() == 1) {
                LoginUser loginUser = new LoginUser(user);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                if (authService.needsPasswordChange(user) && !allowedWhileMustChange(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"code\":403,\"message\":\"请先修改密码后再使用平台\",\"mustChangePassword\":true}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean allowedWhileMustChange(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        return "/api/auth/change-password".equals(uri)
                || "/api/auth/me".equals(uri)
                || "/api/auth/login".equals(uri);
    }

    /** 从 Authorization 头或 URL 参数中取 Token（SSE 用参数传） */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        return null;
    }
}
