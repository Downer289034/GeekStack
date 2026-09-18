package com.community.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.community.media.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * M7-2 上传接口：成功 / 类型拒绝 / 超限 / 未登录 401 / 频率限制。
 * StorageService 用内存桩，CI 不依赖真 MinIO。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MediaUploadIntegrationTest {

    private static final String PREFIX = "m72_";

    private static final byte[] TINY_PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
            (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xFF,
            (byte) 0xFF, 0x3F, 0x00, 0x05, (byte) 0xFE, 0x02,
            (byte) 0xFE, (byte) 0xDC, (byte) 0xCC, 0x59, (byte) 0xE7,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @DynamicPropertySource
    static void rateLimitLow(DynamicPropertyRegistry registry) {
        registry.add("community.media.rate-limit-per-minute", () -> "3");
    }

    @TestConfiguration
    static class InMemoryStorageConfig {
        @Bean
        @Primary
        StorageService inMemoryStorageService() {
            return new InMemoryStorageService();
        }
    }

    /** 测试用内存存储。 */
    static final class InMemoryStorageService implements StorageService {
        private final ConcurrentHashMap<String, byte[]> store = new ConcurrentHashMap<>();

        @Override
        public void put(String key, InputStream data, long size, String contentType) {
            try {
                store.put(key, data.readAllBytes());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void delete(String key) {
            store.remove(key);
        }

        @Override
        public String publicUrl(String key) {
            String path = key.startsWith("media/") ? key.substring("media/".length()) : key;
            return "http://test.local/media/" + path;
        }

        int size() {
            return store.size();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StorageService storageService;

    private String token;

    @BeforeEach
    void init() throws Exception {
        token = register(PREFIX + System.nanoTime());
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    @Test
    void uploadSuccessReturnsUrlWithUuidKey() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", TINY_PNG);
        String resp = mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .param("scene", "cover")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andExpect(jsonPath("$.data.key").isNotEmpty())
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String key = objectMapper.readTree(resp).path("data").path("key").asText();
        assertThat(key).startsWith("media/");
        assertThat(key).doesNotContain("cover.png");
        assertThat(key).matches("media/\\d{4}/\\d{2}/[a-f0-9]{32}\\.png");
        assertThat(((InMemoryStorageService) storageService).size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rejectWrongType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.html", "text/html", "<html>x</html>".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void fakePngWithJpegContentAcceptedAsJpg() throws Exception {
        // 魔数为王：.png 名 + image/png 声明 + JPEG 内容（常见"另存"名实不符）→ 按 JPEG 收，存 .jpg
        byte[] jpeg = new byte[64];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        jpeg[3] = (byte) 0xE0;
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", jpeg);
        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .param("scene", "avatar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.data.key",
                        org.hamcrest.Matchers.matchesPattern("media/\\d{4}/\\d{2}/[a-f0-9]{32}\\.jpg")));
    }

    @Test
    void rejectOversizeForAvatarScene() throws Exception {
        // avatar 上限 2MB：构造略大于 2MB 的假 PNG 头+填充
        byte[] big = new byte[2 * 1024 * 1024 + 64];
        System.arraycopy(TINY_PNG, 0, big, 0, TINY_PNG.length);
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", big);
        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .param("scene", "avatar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void anonymousUploadReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", TINY_PNG);
        mockMvc.perform(multipart("/api/v1/media/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitTriggers429() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", TINY_PNG);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(multipart("/api/v1/media/upload")
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(42900));
    }

    private String register(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@test.local",
                "password", "password123"));
        String resp = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }
}
