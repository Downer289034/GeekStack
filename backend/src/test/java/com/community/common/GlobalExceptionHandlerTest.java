package com.community.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验收标准：任意 Controller 抛异常返回统一 JSON 结构，HTTP 状态码语义正确。
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "probe")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessExceptionReturnsUnifiedBodyWithSemanticStatus() throws Exception {
        mockMvc.perform(get("/probe/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("探针资源不存在"))
                .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    void validationFailureReturns400WithFieldMessage() throws Exception {
        mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("name 不能为空"));
    }

    @Test
    void uncaughtExceptionReturns500WithoutLeakingDetail() throws Exception {
        mockMvc.perform(get("/probe/unknown-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.SYSTEM_ERROR.getMessage()));
    }

    @Test
    void normalPathReturnsOkCode() throws Exception {
        mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
