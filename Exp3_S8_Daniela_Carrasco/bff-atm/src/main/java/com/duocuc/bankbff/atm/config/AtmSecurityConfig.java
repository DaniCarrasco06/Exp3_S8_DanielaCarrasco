package com.duocuc.bankbff.atm.config;

import com.duocuc.bankbff.atm.service.AtmAuthService;
import com.duocuc.bankbff.core.security.RolesJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AtmSecurityConfig {

    private final AtmAuthService atmAuthService;

    public AtmSecurityConfig(AtmAuthService atmAuthService) {
        this.atmAuthService = atmAuthService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AtmSessionFilter sessionFilter = new AtmSessionFilter(atmAuthService.getTokenProvider());

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/atm/auth/**").hasAuthority("SCOPE_atm.device")
                    .requestMatchers("/api/atm/**").hasRole("ATM_DEVICE")
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(rs -> rs.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(RolesJwtAuthenticationConverter.create())))
            .addFilterAfter(sessionFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
