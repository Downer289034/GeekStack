package com.community.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);
    private static final String DEFAULT_BASE = "http://127.0.0.1:8090";

    /**
     * ${AI_SERVICE_URL} 与 spring.config.import 同文档时会提前落成默认本机地址。
     * 仅当仍是默认值时用根目录 .env / 环境变量回填；TestPropertySource 等显式值不覆盖。
     */
    public AiConfig(AiProperties properties, Environment environment) {
        String current = trimSlash(properties.getBaseUrl());
        if (DEFAULT_BASE.equals(current)) {
            String resolved = firstHttpUrl(
                    environment.getProperty("AI_SERVICE_URL"),
                    readRootEnv("AI_SERVICE_URL"));
            if (resolved != null) {
                properties.setBaseUrl(resolved);
            }
        }
        log.info("AI gateway base={}", properties.getBaseUrl());
    }

    private static String firstHttpUrl(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String value = candidate.trim();
            if (value.startsWith("http://") || value.startsWith("https://")) {
                return value;
            }
        }
        return null;
    }

    private static String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String readRootEnv(String key) {
        for (Path path : new Path[]{Path.of(".env"), Path.of("..", ".env")}) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eq = trimmed.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    if (key.equals(trimmed.substring(0, eq).trim())) {
                        return trimmed.substring(eq + 1).trim();
                    }
                }
            } catch (IOException ignored) {
                // 缺文件时保持默认
            }
        }
        return null;
    }
}
