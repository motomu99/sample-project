package com.example.boxwrapper.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * セキュリティ設定
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyFilter() {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registrationBean =
            new FilterRegistrationBean<>();

        registrationBean.setFilter(apiKeyAuthenticationFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);

        return registrationBean;
    }
}
