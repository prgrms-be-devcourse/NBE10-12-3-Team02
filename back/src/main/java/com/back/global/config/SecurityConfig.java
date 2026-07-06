package com.back.global.config;

import com.back.global.exception.ErrorCode;
import com.back.global.rsData.RsData;
import com.back.global.security.filter.CustomAuthenticationFilter;
import com.back.global.security.oauth2.service.CustomOAuth2UserService;
import com.back.global.security.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.back.global.security.oauth2.loginhandler.OAuth2LoginFailureHandler;
import com.back.global.security.oauth2.loginhandler.OAuth2LoginSuccessHandler;
import com.back.global.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http

                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/h2-console/**").permitAll()
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/*/concerts",
                                        "/api/*/concerts/*",
                                        "/api/*/schedules/**",
                                        "/api/*/users/check-id"
                                ).permitAll()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/*/auth/login",
                                        "/api/*/auth/logout",
                                        "/api/*/auth/refresh",
                                        "/api/*/users/signup",
                                        "/api/*/auth/restore"
                                ).permitAll()
                                .requestMatchers("/api/*/**").authenticated()
                                .anyRequest().permitAll()
                )
                .headers(
                        headers -> headers
                                .frameOptions(
                                        HeadersConfigurer.FrameOptionsConfig::sameOrigin
                                )
                )
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(401);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            new RsData<Void>(
                                                                    ErrorCode.AUTH_LOGIN_REQUIRED.getResultCode(),
                                                                    ErrorCode.AUTH_LOGIN_REQUIRED.getMessage()
                                                            ),
                                                            "{\"resultCode\":\"401-1\",\"msg\":\"로그인 후 이용해주세요.\",\"data\":null}"
                                                    )
                                            );
                                        }
                                )
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) -> {
                                            response.setContentType("application/json;charset=UTF-8");

                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            new RsData<Void>(
                                                                    ErrorCode.AUTH_FORBIDDEN.getResultCode(),
                                                                    ErrorCode.AUTH_FORBIDDEN.getMessage()
                                                            ),
                                                            "{\"resultCode\":\"403-1\",\"msg\":\"권한이 없습니다.\",\"data\":null}"
                                                    )
                                            );
                                        }
                                )
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(authorizationRequestRepository)
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                );
        return http.build();
    }
}
