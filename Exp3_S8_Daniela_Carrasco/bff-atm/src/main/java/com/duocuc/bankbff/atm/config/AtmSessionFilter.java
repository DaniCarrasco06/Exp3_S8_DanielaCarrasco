package com.duocuc.bankbff.atm.config;

import com.duocuc.bankbff.core.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class AtmSessionFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER = "X-Atm-Session";

    private final JwtTokenProvider sessionTokenProvider;

    public AtmSessionFilter(JwtTokenProvider sessionTokenProvider) {
        this.sessionTokenProvider = sessionTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        String session = request.getHeader(SESSION_HEADER);

        if (session != null && current instanceof JwtAuthenticationToken deviceAuth
                && hasDeviceScope(deviceAuth) && sessionTokenProvider.isValid(session)) {
            Claims claims = sessionTokenProvider.parseClaims(session);
            List<SimpleGrantedAuthority> authorities = sessionTokenProvider.extractRoles(claims).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasDeviceScope(JwtAuthenticationToken auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> "SCOPE_atm.device".equals(a.getAuthority()));
    }
}
