package com.community.forum.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.common.PageResponse;
import com.community.common.TagNormalizer;
import com.community.event.ContentCreatedEvent;
import com.community.forum.dto.CategoryResponse;
import com.community.forum.dto.CreatePostRequest;
import com.community.forum.dto.PostCardResponse;
import com.community.forum.dto.PostDetailResponse;
import com.community.forum.dto.UpdatePostRequest;
import com.community.forum.entity.Post;
import com.community.forum.entity.PostCategory;
import com.community.forum.mapper.PostCategoryMapper;
import com.community.forum.mapper.PostMapper;
import com.community.rag.event.RagContentChangedEvent;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 论坛帖子：发帖 / 编辑 / 删除（逻辑删除）/ 分类列表 / 分类过滤 / 最新与最热分页。
 * 最热口径：置顶优先，按 reply_count*3 + view_count 加权倒序（权重随 T12 回帖联动再校准）。
 */
@Service
public class PostService {

    private static final String SORT_HOT = "hot";
    private static final int MAX_TAGS = 5;

    private final PostMapper postMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public PostService(PostMapper postMapper, PostCategoryMapper postCategoryMapper,
                       UserService userService, ApplicationEventPublisher eventPublisher) {
        this.postMapper = postMapper;
        this.postCategoryMapper = postCategoryMapper;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /** 分类列表（按 sort_order 升序）。 */
    public List<CategoryResponse> listCategories() {
        List<PostCategory> categories = postCategoryMapper.selectList(
                Wrappers.<PostCategory>lambdaQuery().orderByAsc(PostCategory::getSortOrder));
        return categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getSortOrder()))
                .toList();
    }

    /** 发帖。 */
    public PostDetailResponse create(String username, CreatePostRequest request) {
        long userId = requireUserId(username);
        requireCategory(request.categoryId());
        Post post = new Post();
        post.setUserId(userId);
        post.setCategoryId(request.categoryId());
        post.setTitle(request.title());
        post.setContentMd(request.contentMd());
        post.setTags(TagNormalizer.normalize(request.tags(), MAX_TAGS));
        post.setViewCount(0L);
        post.setReplyCount(0L);
        post.setIsPinned(0);
        post.setIsFeatured(0);
        post.setIsHot(0);
        postMapper.insert(post);
        // T14.6：发布 post.created 事件（content_text = 正文全文，截断由 Listener 处理）
        eventPublisher.publishEvent(
                new ContentCreatedEvent.PostCreated(this, post.getId(), post.getContentMd(), userId));
        eventPublisher.publishEvent(RagContentChangedEvent.upsert(this, "post", post.getId()));
        return toDetail(post);
    }

    /** 编辑帖子，仅限作者本人（分类不可改）。 */
    public PostDetailResponse update(long id, String username, UpdatePostRequest request) {
        Post post = requireOwned(id, username);
        post.setTitle(request.title());
        post.setContentMd(request.contentMd());
        // tags 字段：null 表示不改；非 null（含空数组）表示覆盖
        if (request.tags() != null) {
            post.setTags(TagNormalizer.normalize(request.tags(), MAX_TAGS));
        }
        postMapper.updateById(post);
        eventPublisher.publishEvent(RagContentChangedEvent.upsert(this, "post", post.getId()));
        return toDetail(post);
    }

    /** 删除帖子（逻辑删除，deleted=1），仅限作者本人；列表与详情随之不可见。 */
    public void delete(long id, String username) {
        Post post = requireOwned(id, username);
        postMapper.deleteById(post.getId());
        eventPublisher.publishEvent(RagContentChangedEvent.delete(this, "post", id));
    }

    /**
     * 管理员删帖（逻辑删除，口径与作者自删一致）。
     *
     * @return 被删帖实体（供审计落日志：目标帖/作者）
     */
    public Post deleteByAdmin(long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        postMapper.deleteById(post.getId());
        eventPublisher.publishEvent(RagContentChangedEvent.delete(this, "post", id));
        return post;
    }

    /**
     * 管理员设置加精（0/1，幂等）。
     *
     * @return 更新后的帖子（供审计）
     */
    public Post setFeaturedByAdmin(long id, int featured) {
        Post post = requireExisting(id);
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .eq(Post::getId, id)
                .set(Post::getIsFeatured, featured));
        post.setIsFeatured(featured);
        return post;
    }

    /**
     * 管理员设置置顶（0/1，幂等）。
     *
     * @return 更新后的帖子（供审计）
     */
    public Post setPinnedByAdmin(long id, int pinned) {
        Post post = requireExisting(id);
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .eq(Post::getId, id)
                .set(Post::getIsPinned, pinned));
        post.setIsPinned(pinned);
        return post;
    }

    /**
     * 帖子分页：可选分类 slug / 标签过滤；sort=latest（默认）按最新，hot 按最热加权。置顶恒在最前。
     */
    public PageResponse<PostCardResponse> page(long current, long size, String categorySlug, String sort,
                                               String tag) {
        Long categoryId = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            PostCategory category = postCategoryMapper.selectOne(
                    Wrappers.<PostCategory>lambdaQuery().eq(PostCategory::getSlug, categorySlug.trim()));
            if (category == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在");
            }
            categoryId = category.getId();
        }
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        // 逻辑删除（deleted=0）由 MyBatis-Plus 全局配置自动追加
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (tag != null && !tag.isBlank()) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag.trim());
        }
        boolean hot = SORT_HOT.equalsIgnoreCase(sort);
        if (hot) {
            wrapper.orderByDesc("is_pinned", "reply_count * 3 + view_count", "id");
        } else {
            wrapper.orderByDesc("is_pinned", "created_at", "id");
        }
        Page<Post> page = postMapper.selectPage(Page.of(current, size), wrapper);
        return PageResponse.from(page.convert(this::toCard));
    }

    /** 帖子详情（逻辑删除后不可见）；浏览数原子 +1（与 share 模块口径一致）。 */
    public PostDetailResponse detail(long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .eq(Post::getId, id)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        return toDetail(post);
    }

    /** 某用户的帖子（主页聚合用，限量取前 N 条，仅未逻辑删除——帖无隐藏态，口径天然一致）。 */
    public List<PostCardResponse> listByUser(long userId, int limit) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        // 逻辑删除过滤由 MP 全局配置自动追加；selectList 不会触发分页插件，LIMIT 由 last 提供
        wrapper.orderByDesc("created_at").orderByDesc("id");
        wrapper.last("LIMIT " + limit);
        return postMapper.selectList(wrapper).stream().map(this::toCard).toList();
    }

    /** T-DATA-1：某用户发帖分页（未逻辑删除；供管家只读历史）。 */
    public PageResponse<Post> pageByUser(long userId, long current, long size) {
        Page<Post> page = postMapper.selectPage(
                Page.of(current, size),
                Wrappers.<Post>lambdaQuery()
                        .eq(Post::getUserId, userId)
                        .orderByDesc(Post::getCreatedAt)
                        .orderByDesc(Post::getId));
        return PageResponse.from(page);
    }

    /** T-DATA-1：全站帖子量（未逻辑删除）。 */
    public long countAll() {
        return postMapper.selectCount(Wrappers.lambdaQuery());
    }

    /** T21.1：委托 PostCardResponse.from 统一入口。 */
    private PostCardResponse toCard(Post post) {
        return PostCardResponse.from(post);
    }

    private PostDetailResponse toDetail(Post post) {
        List<String> tags = post.getTags() == null ? List.of() : List.copyOf(post.getTags());
        return new PostDetailResponse(post.getId(), post.getUserId(), post.getCategoryId(), post.getTitle(),
                post.getContentMd(), tags, post.getViewCount(), post.getReplyCount(), post.getLastReplyAt(),
                post.getIsPinned(), post.getIsFeatured(), post.getIsHot(), post.getCreatedAt());
    }

    private Post requireExisting(long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private Post requireOwned(long id, String username) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        if (!post.getUserId().equals(requireUserId(username))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅限作者本人操作");
        }
        return post;
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || postCategoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在");
        }
    }

    private long requireUserId(String username) {
        UserResponse user = userService.me(username);
        return user.id();
    }
}
