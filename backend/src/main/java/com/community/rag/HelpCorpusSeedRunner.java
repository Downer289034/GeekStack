package com.community.rag;

import com.community.rag.model.RagDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 平台帮助语料一次性灌库（community.rag.seed-help-on-startup=true 时执行）。
 */
@Component
public class HelpCorpusSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HelpCorpusSeedRunner.class);

    private final RagProperties properties;
    private final RagIndexService ragIndexService;
    private final ObjectMapper objectMapper;

    public HelpCorpusSeedRunner(RagProperties properties, RagIndexService ragIndexService,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.ragIndexService = ragIndexService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.isSeedHelpOnStartup()) {
            return;
        }
        seedFromClasspath("rag/help-corpus.json");
    }

    public int seedFromClasspath(String classpathLocation) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            List<Map<String, Object>> rows = objectMapper.readValue(in, new TypeReference<>() {
            });
            int n = 0;
            for (Map<String, Object> row : rows) {
                String id = String.valueOf(row.get("content_id"));
                RagDocument doc = new RagDocument(
                        id.startsWith("jkz_") ? id : "jkz_" + id,
                        String.valueOf(row.getOrDefault("content_type", "help")),
                        String.valueOf(row.getOrDefault("title", "")),
                        String.valueOf(row.getOrDefault("url", "")),
                        ((Number) row.getOrDefault("author_id", 0)).longValue(),
                        String.valueOf(row.getOrDefault("created_at", "")),
                        "public",
                        String.valueOf(row.getOrDefault("body_md", "")),
                        Map.of("seed", true));
                ragIndexService.indexDocument(doc);
                n++;
            }
            log.info("RAG help corpus seeded: {} docs from {}", n, classpathLocation);
            return n;
        } catch (Exception e) {
            log.warn("RAG help corpus seed 失败(不阻塞启动): {}", e.getMessage());
            return 0;
        }
    }
}
