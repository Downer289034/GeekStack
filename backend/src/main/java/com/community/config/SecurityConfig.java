package com.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 无状态安全配置：JWT 过滤器 + 统一 401 响应；健康检查、认证接口与广场只读接口放行，其余接口要求认证。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 无状态过滤链：放行健康检查与认证接口，其余要求 JWT 认证。 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   RestAuthenticationEntryPoint entryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // OAuth 回调与授权码转交：双语义由是否携带 JWT 决定，匿名必须可达
                        .requestMatchers("/api/v1/oauth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shares", "/api/v1/shares/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/categories",
                                "/api/v1/posts/*", "/api/v1/posts/*/comments",
                                "/api/v1/shares/*/comments", "/api/v1/users/*/profile",
                                "/api/v1/users/*/repos", "/api/v1/hot",
                                "/api/v1/search", "/api/v1/tags").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt 密码编码器（强度默认 10）。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
