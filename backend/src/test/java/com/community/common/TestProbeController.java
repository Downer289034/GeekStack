package com.community.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅测试用探针控制器：触发各类异常以验证统一响应结构，不进生产代码。
 */
@RestController
public class TestProbeController {

    @GetMapping("/probe/business")
    public Result<Void> business() {
        throw new BusinessException(ErrorCode.NOT_FOUND, "探针资源不存在");
    }

    @GetMapping("/probe/unknown-error")
    public Result<Void> unknown() {
        throw new IllegalStateException("boom");
    }

    @PostMapping("/probe/valid")
    public Result<Void> valid(@Valid @RequestBody ProbeBody body) {
        return Result.ok();
    }

    @Data
    static class ProbeBody {
        @NotBlank(message = "不能为空")
        private String name;
    }
}
