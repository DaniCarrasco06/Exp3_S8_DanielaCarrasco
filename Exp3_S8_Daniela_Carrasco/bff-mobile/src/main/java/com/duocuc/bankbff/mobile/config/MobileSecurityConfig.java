package com.duocuc.bankbff.mobile.config;

import com.duocuc.bankbff.core.security.RolesJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class MobileSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/mobile/**").hasRole("MOBILE_USER")
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(rs -> rs.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(RolesJwtAuthenticationConverter.create())));

        return http.build();
    }
}
