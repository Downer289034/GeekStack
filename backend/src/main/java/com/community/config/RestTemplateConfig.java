package com.community.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate bean（Webhook 推送用）。
 *
 * <p>必须配置短超时：无超时的推送在目标不可达时会挂死默认 @Async 线程池，
 * 饿死同池的站内通知等异步任务（T-BE-1 根因）。
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 带连接/读取超时的 RestTemplate，超时后抛 ResourceAccessException，由推送侧 WARN 吞掉。
     */
    @Bean
    public RestTemplate restTemplate(
            @Value("${community.event.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${community.event.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new RestTemplate(factory);
    }
}
