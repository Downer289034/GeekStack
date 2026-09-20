package com.community.rag;

import com.community.forum.entity.Post;
import com.community.forum.mapper.PostMapper;
import com.community.rag.model.RagDocument;
import com.community.rag.model.RagPoint;
import com.community.share.entity.ProjectShare;
import com.community.share.mapper.ProjectShareMapper;
import com.community.skill.entity.Skill;
import com.community.skill.mapper.SkillMapper;
import com.community.user.entity.User;
import com.community.user.mapper.UserMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 语料索引：过滤公开内容 → 分块 → embedding → upsert/delete。
 */
@Service
public class RagIndexService {

    private static final Logger log = LoggerFactory.getLogger(RagIndexService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RagProperties properties;
    private final MarkdownChunker chunker;
    private final EmbeddingClient embeddingClient;
    private final RagVectorStore vectorStore;
    private final PostMapper postMapper;
    private final ProjectShareMapper shareMapper;
    private final SkillMapper skillMapper;
    private final UserMapper userMapper;

    public RagIndexService(RagProperties properties, MarkdownChunker chunker,
                           EmbeddingClient embeddingClient, RagVectorStore vectorStore,
                           PostMapper postMapper, ProjectShareMapper shareMapper,
                           SkillMapper skillMapper, UserMapper userMapper) {
        this.properties = properties;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.postMapper = postMapper;
        this.shareMapper = shareMapper;
        this.skillMapper = skillMapper;
        this.userMapper = userMapper;
    }

    public void upsert(String contentType, long contentId) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            Optional<RagDocument> doc = loadPublicDocument(contentType, contentId);
            if (doc.isEmpty()) {
                // 不可索引（HIDDEN/缺失/作者封禁）→ 确保向量侧清理
                delete(contentType, contentId);
                return;
            }
            indexDocument(doc.get());
        } catch (Exception e) {
            log.warn("RAG upsert 失败(不阻塞): type={} id={} error={}", contentType, contentId, e.getMessage());
        }
    }

    public void delete(String contentType, long contentId) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String parent = parentContentId(contentType, contentId);
            vectorStore.deleteByParentContentId(parent);
            log.info("RAG delete: parent={}", parent);
        } catch (Exception e) {
            log.warn("RAG delete 失败(不阻塞): type={} id={} error={}", contentType, contentId, e.getMessage());
        }
    }

    /** 帮助语料 / 测试直接灌入。 */
    public void indexDocument(RagDocument doc) {
        List<String> texts = chunker.chunk(doc.bodyMd());
        if (texts.isEmpty() && doc.title() != null && !doc.title().isBlank()) {
            texts = List.of(doc.title());
        }
        if (texts.isEmpty()) {
            vectorStore.deleteByParentContentId(doc.parentContentId());
            return;
        }
        // 先删旧 chunk，再写新（编辑换库）
        vectorStore.deleteByParentContentId(doc.parentContentId());
        List<List<Float>> vectors = embeddingClient.embed(texts);
        String authorMasked = maskAuthor(doc.authorId());
        List<RagVectorStore.RagPointView> points = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            Map<String, Object> payload = RagPoint.buildPayload(
                    doc.parentContentId(),
                    properties.getSourceSystem(),
                    doc.contentType(),
                    doc.textUrl(),
                    authorMasked,
                    doc.createdAt(),
                    doc.visibility(),
                    properties.getEmbeddingModel(),
                    doc.title(),
                    i,
                    texts.get(i),
                    doc.extra());
            String pointId = doc.parentContentId() + "#" + i;
            points.add(new RagVectorStore.RagPointView(pointId, vectors.get(i), payload));
        }
        vectorStore.ensureCollection();
        vectorStore.upsert(points);
        log.info("RAG upsert: parent={} chunks={}", doc.parentContentId(), points.size());
    }

    public static String parentContentId(String contentType, long contentId) {
        return "jkz_" + contentType + "_" + contentId;
    }

    public static String maskAuthor(long authorId) {
        int digest = Math.floorMod(Long.hashCode(authorId) * 31 + 17, 10_000_000);
        return String.format("u_%07d", digest);
    }

    private Optional<RagDocument> loadPublicDocument(String contentType, long contentId) {
        return switch (contentType) {
            case "post" -> loadPost(contentId);
            case "share" -> loadShare(contentId);
            case "skill" -> loadSkill(contentId);
            default -> {
                log.warn("RAG 不支持的 contentType={}", contentType);
                yield Optional.empty();
            }
        };
    }

    private Optional<RagDocument> loadPost(long id) {
        Post post = postMapper.selectById(id);
        if (post == null || !isAuthorIndexable(post.getUserId())) {
            return Optional.empty();
        }
        String body = (post.getTitle() == null ? "" : post.getTitle() + "\n")
                + (post.getContentMd() == null ? "" : post.getContentMd());
        return Optional.of(new RagDocument(
                parentContentId("post", id),
                "post",
                post.getTitle(),
                publicUrl("/posts/" + id),
                post.getUserId(),
                formatTime(post.getCreatedAt()),
                "public",
                body,
                Map.of()));
    }

    private Optional<RagDocument> loadShare(long id) {
        ProjectShare share = shareMapper.selectById(id);
        if (share == null || !"PUBLISHED".equals(share.getStatus()) || !isAuthorIndexable(share.getUserId())) {
            return Optional.empty();
        }
        String body = (share.getTitle() == null ? "" : share.getTitle() + "\n")
                + (share.getSummary() == null ? "" : share.getSummary() + "\n")
                + (share.getContentMd() == null ? "" : share.getContentMd());
        return Optional.of(new RagDocument(
                parentContentId("share", id),
                "share",
                share.getTitle(),
                publicUrl("/shares/" + id),
                share.getUserId(),
                formatTime(share.getCreatedAt()),
                "public",
                body,
                Map.of()));
    }

    private Optional<RagDocument> loadSkill(long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null || !"PUBLISHED".equals(skill.getStatus()) || !isAuthorIndexable(skill.getUserId())) {
            return Optional.empty();
        }
        String body = (skill.getTitle() == null ? "" : skill.getTitle() + "\n")
                + (skill.getSummary() == null ? "" : skill.getSummary() + "\n")
                + (skill.getContentMd() == null ? "" : skill.getContentMd());
        return Optional.of(new RagDocument(
                parentContentId("skill", id),
                "skill",
                skill.getTitle(),
                publicUrl("/skills/" + id),
                skill.getUserId(),
                formatTime(skill.getCreatedAt()),
                "public",
                body,
                Map.of("version", skill.getVersion() == null ? "" : skill.getVersion())));
    }

    private boolean isAuthorIndexable(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && (user.getStatus() == null || user.getStatus() == 0);
    }

    private String publicUrl(String path) {
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return path;
        }
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }

    private static String formatTime(java.time.LocalDateTime t) {
        return t == null ? "" : ISO.format(t);
    }
}
