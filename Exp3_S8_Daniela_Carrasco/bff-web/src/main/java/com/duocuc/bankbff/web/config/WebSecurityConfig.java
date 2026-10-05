package com.duocuc.bankbff.web.config;

import com.duocuc.bankbff.core.security.RolesJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/web/**").hasRole("WEB_USER")
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(rs -> rs.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(RolesJwtAuthenticationConverter.create())));

        return http.build();
    }
}
