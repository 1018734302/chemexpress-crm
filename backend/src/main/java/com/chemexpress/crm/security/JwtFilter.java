package com.chemexpress.crm.security;

import com.chemexpress.crm.entity.User;
import com.chemexpress.crm.mapper.UserMapper;
import com.chemexpress.crm.service.AppMetaService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppMetaService appMetaService;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                long tokenVersion = claims.get("dv", Number.class).longValue();
                if (tokenVersion == appMetaService.getDataVersion()) {
                    Long userId = Long.valueOf(claims.getSubject());
                    User account = userMapper.selectById(userId);
                    if (account != null && !Boolean.FALSE.equals(account.getEnabled())) {
                        CurrentUser user = new CurrentUser(
                                account.getId(),
                                account.getUsername(),
                                account.getName(),
                                account.getRole(),
                                account.getRegion(),
                                tokenVersion
                        );
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
