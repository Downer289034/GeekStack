package com.community.media.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 图片三一致校验：扩展名白名单 + Content-Type + 魔数。
 */
public final class ImageValidator {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private static final Map<String, String> EXT_TO_MIME = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif");

    private ImageValidator() {
    }

    /**
     * 校验结果。
     *
     * @param extension   归一化扩展名（jpeg→jpg 存盘用 jpg）
     * @param contentType 规范化 MIME
     */
    public record ValidatedImage(String extension, String contentType) {
    }

    /**
     * 三一致校验。
     *
     * @param originalFilename 原始文件名（仅取扩展名，不入库）
     * @param declaredType     客户端 Content-Type
     * @param header           文件头至少 12 字节
     * @return 校验通过后的扩展名与 MIME
     */
    public static ValidatedImage validate(String originalFilename, String declaredType, byte[] header) {
        String ext = extractExtension(originalFilename);
        if (ext == null || !ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 jpg/png/webp/gif");
        }
        String normalizedExt = "jpeg".equals(ext) ? "jpg" : ext;
        String expectedMime = EXT_TO_MIME.get(normalizedExt.equals("jpg") ? "jpg" : normalizedExt);
        // jpeg 扩展名也映射 image/jpeg
        if ("jpg".equals(normalizedExt)) {
            expectedMime = "image/jpeg";
        }

        String mime = declaredType == null ? "" : declaredType.trim().toLowerCase(Locale.ROOT);
        if (mime.contains(";")) {
            mime = mime.substring(0, mime.indexOf(';')).trim();
        }
        if (!expectedMime.equals(mime)
                && !(("jpg".equals(normalizedExt) || "jpeg".equals(ext)) && "image/jpeg".equals(mime))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Content-Type 与扩展名不一致");
        }

        String magicMime = sniffMagic(header);
        if (magicMime == null || !magicMime.equals(expectedMime)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容不是合法图片");
        }
        return new ValidatedImage(normalizedExt, expectedMime);
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

    private static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String name = filename.trim();
        // 拒绝路径分隔与双扩展名探测（取最后一段）
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        // 拒绝 x.jpg.html 一类：扩展名本身再含点已不可能；再查是否含非字母
        if (!ext.chars().allMatch(Character::isLetter)) {
            return null;
        }
        return ext;
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
