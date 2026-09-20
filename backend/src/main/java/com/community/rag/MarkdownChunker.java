package com.community.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Markdown 粗清洗 + token 窗口分块（定稿 512/50）。
 * token 估算：CJK≈1 token/字，其它≈1 token/4 chars（与 cl100k 量级接近，长文复测见任务说明）。
 */
@Component
public class MarkdownChunker {

    private static final Pattern MD_STRIP = Pattern.compile(
            "```[\\s\\S]*?```|`[^`]+`|!\\[[^\\]]*\\]\\([^)]+\\)|\\[[^\\]]*\\]\\([^)]+\\)|[#>*_\\-]{1,}");

    private final RagProperties properties;

    public MarkdownChunker(RagProperties properties) {
        this.properties = properties;
    }

    public List<String> chunk(String markdown) {
        return chunk(markdown, properties.getChunkSizeTokens(), properties.getChunkOverlapTokens());
    }

    public List<String> chunk(String markdown, int sizeTokens, int overlapTokens) {
        String plain = stripMarkdown(markdown);
        if (plain.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokenize(plain);
        if (tokens.isEmpty()) {
            return List.of();
        }
        int size = Math.max(1, sizeTokens);
        int overlap = Math.max(0, Math.min(overlapTokens, size - 1));
        int step = Math.max(1, size - overlap);
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += step) {
            int end = Math.min(tokens.size(), start + size);
            String piece = String.join("", tokens.subList(start, end)).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= tokens.size()) {
                break;
            }
        }
        return chunks;
    }

    static String stripMarkdown(String md) {
        String text = MD_STRIP.matcher(md == null ? "" : md).replaceAll(" ");
        return text.replaceAll("\\s+", " ").trim();
    }

    /** 粗粒度 token：每个 CJK 字单独；连续非 CJK 按约 4 字符切一段。 */
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder latin = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (isCjk(cp)) {
                flushLatin(latin, tokens);
                tokens.add(new String(Character.toChars(cp)));
            } else {
                latin.appendCodePoint(cp);
                if (latin.length() >= 4) {
                    tokens.add(latin.toString());
                    latin.setLength(0);
                }
            }
        }
        flushLatin(latin, tokens);
        return tokens;
    }

    private static void flushLatin(StringBuilder latin, List<String> tokens) {
        if (latin.length() > 0) {
            tokens.add(latin.toString());
            latin.setLength(0);
        }
    }

    private static boolean isCjk(int cp) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }
}
