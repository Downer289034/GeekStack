package com.community.event;

import com.community.event.dto.EventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EventPayloadSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void eventPayloadSerializesCorrectly() throws Exception {
        EventPayload payload = new EventPayload(
                "post_101", "post", "测试内容", "user_55",
                EventPayload.Metadata.created("post", false));

        String json = objectMapper.writeValueAsString(payload);
        assertThat(json).contains("\"contentId\":\"post_101\"");
        assertThat(json).contains("\"contentType\":\"post\"");
        assertThat(json).contains("\"contentText\":\"测试内容\"");
        assertThat(json).contains("\"authorId\":\"user_55\"");
        assertThat(json).contains("\"source\":\"jikezhan\"");

        // 反序列化验证往返
        EventPayload parsed = objectMapper.readValue(json, EventPayload.class);
        assertThat(parsed.contentId()).isEqualTo("post_101");
    }
}
