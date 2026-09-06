package com.community.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.URL;

/**
 * 创建/编辑项目分享请求（repo_url、demo_url 缺省可填，但填写时必须是合法 URL）。
 */
public record SharePayload(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank @Size(max = 500)
        String summary,

        @NotBlank
        String contentMd,

        @Size(max = 500)
        @URL(message = "仓库地址必须是合法 URL")
        String repoUrl,

        @Size(max = 500)
        @URL(message = "在线地址必须是合法 URL")
        String demoUrl,

        @Size(max = 10, message = "标签最多 10 个")
        List<String> tags
) {
}
