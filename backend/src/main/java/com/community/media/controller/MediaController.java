package com.community.media.controller;

import com.community.common.Result;
import com.community.media.dto.MediaUploadResponse;
import com.community.media.service.MediaUploadService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * M7-2 图片上传：登录 + multipart；scene 分档大小。
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaUploadService mediaUploadService;

    /**
     * 构造。
     *
     * @param mediaUploadService 上传服务
     */
    public MediaController(MediaUploadService mediaUploadService) {
        this.mediaUploadService = mediaUploadService;
    }

    /**
     * 上传单图。字段名 {@code file}；可选 {@code scene=editor|cover|avatar}。
     *
     * @param file           文件
     * @param scene          场景
     * @param authentication 登录态
     * @return url/key/contentType/size
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<MediaUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scene", required = false, defaultValue = "editor") String scene,
            Authentication authentication) {
        return Result.ok(mediaUploadService.upload(authentication.getName(), file, scene));
    }
}
