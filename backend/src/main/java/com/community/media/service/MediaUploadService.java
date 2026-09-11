package com.community.media.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.media.dto.MediaUploadResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体上传编排：限流 → 校验 → 生成 UUID 键 → 落盘。
 */
@Service
public class MediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadService.class);
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageService storageService;
    private final UploadRateLimiter rateLimiter;

    /**
     * 构造。
     *
     * @param storageService 存储
     * @param rateLimiter    限流
     */
    public MediaUploadService(StorageService storageService, UploadRateLimiter rateLimiter) {
        this.storageService = storageService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 上传图片。
     *
     * @param username 登录用户
     * @param file     multipart 文件
     * @param scene    editor|cover|avatar
     * @return 上传结果
     */
    public MediaUploadResponse upload(String username, MultipartFile file, String scene) {
        rateLimiter.check(username);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        long maxBytes = ImageValidator.maxBytesForScene(scene);
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件超过大小限制");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法读取上传文件");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (bytes.length > maxBytes) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件超过大小限制");
        }

        byte[] header = new byte[Math.min(12, bytes.length)];
        System.arraycopy(bytes, 0, header, 0, header.length);
        ImageValidator.ValidatedImage validated =
                ImageValidator.validate(file.getOriginalFilename(), file.getContentType(), header);

        String key = buildObjectKey(validated.extension());
        storageService.put(key, new ByteArrayInputStream(bytes), bytes.length, validated.contentType());
        String url = storageService.publicUrl(key);
        log.info("媒体上传成功: user={}, key={}, size={}, scene={}",
                username, key, bytes.length, scene == null ? "editor" : scene);
        return new MediaUploadResponse(url, key, validated.contentType(), bytes.length);
    }

    private static String buildObjectKey(String ext) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ym = LocalDate.now().format(YM);
        return "media/" + ym + "/" + uuid + "." + ext;
    }
}
