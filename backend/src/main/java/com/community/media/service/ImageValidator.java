package com.community.media.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.util.Locale;

/**
 * 图片魔数校验：以文件头真实格式为准（魔数为王）。
 *
 * <p>策略（T-PROFILE-BE 用户反馈修复）：用户文件常出现「扩展名 .png 但内容实为 JPEG」等
 * 名实不符情况（聊天工具/截图软件另存所致），原「扩展名+Content-Type+魔数三一致」会误拒此类
 * 完全可显示的图片。现仅信任魔数：真实格式在白名单（jpg/png/gif/webp）内即通过，并按真实格式
 * 存盘；伪装攻击（EXE/HTML 改名 .png）魔数不在白名单，依旧拒绝——安全性不降。
 */
public final class ImageValidator {

    private ImageValidator() {
    }

    /**
     * 校验结果。
     *
     * @param extension   按魔数识别的真实扩展名（jpeg→jpg 存盘用 jpg）
     * @param contentType 真实 MIME
     */
    public record ValidatedImage(String extension, String contentType) {
    }

    /**
     * 魔数校验。
     *
     * @param header 文件头至少 12 字节（WebP 需要读满 12 字节）
     * @return 校验通过后的扩展名与 MIME
     */
    public static ValidatedImage validate(byte[] header) {
        String magicMime = sniffMagic(header);
        if (magicMime == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容不是合法图片");
        }
        return new ValidatedImage(mimeToExt(magicMime), magicMime);
    }

    /**
     * 按场景返回字节上限。
     *
     * @param scene editor|cover|avatar
     * @return 最大字节数
     */
    public static long maxBytesForScene(String scene) {
        String s = scene == null || scene.isBlank() ? "editor" : scene.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "cover" -> 5L * 1024 * 1024;
            case "avatar" -> 2L * 1024 * 1024;
            case "editor" -> 10L * 1024 * 1024;
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "scene 仅支持 editor|cover|avatar");
        };
    }

    private static String mimeToExt(String mime) {
        return switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容不是合法图片");
        };
    }

    private static String sniffMagic(byte[] header) {
        if (header == null || header.length < 12) {
            return null;
        }
        // JPEG FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG 89 50 4E 47 0D 0A 1A 0A
        if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return "image/png";
        }
        // GIF87a / GIF89a
        if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8'
                && (header[4] == '7' || header[4] == '9') && header[5] == 'a') {
            return "image/gif";
        }
        // WebP: RIFF....WEBP
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
