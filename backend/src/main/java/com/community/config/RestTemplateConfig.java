package com.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate bean（Webhook 推送用）。
 */
@Configuration
public class RestTemplateConfig {

    /** 默认 RestTemplate（超时由 Agent 侧控制）。 */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
