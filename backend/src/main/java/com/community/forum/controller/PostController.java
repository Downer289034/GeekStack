package com.community.forum.controller;

import com.community.common.PageResponse;
import com.community.common.Result;
import com.community.forum.dto.CategoryResponse;
import com.community.forum.dto.CreatePostRequest;
import com.community.forum.dto.PostCardResponse;
import com.community.forum.dto.PostDetailResponse;
import com.community.forum.dto.UpdatePostRequest;
import com.community.forum.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 论坛接口：分类/列表/详情公开（GET），发帖/编辑/删除需认证。
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** 分类列表（公开）。 */
    @GetMapping("/categories")
    public Result<List<CategoryResponse>> categories() {
        return Result.ok(postService.listCategories());
    }

    /** 帖子分页（公开）：category=分类 slug，tag=精确标签，sort=latest|hot。 */
    @GetMapping
    public Result<PageResponse<PostCardResponse>> page(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String tag,
                                                       @RequestParam(defaultValue = "latest") String sort) {
        return Result.ok(postService.page(current, size, category, sort, tag));
    }

    /** 帖子详情（公开）。 */
    @GetMapping("/{id}")
    public Result<PostDetailResponse> detail(@PathVariable long id) {
        return Result.ok(postService.detail(id));
    }

    /** 发帖。 */
    @PostMapping
    public Result<PostDetailResponse> create(@Valid @RequestBody CreatePostRequest request,
                                             Authentication authentication) {
        return Result.ok(postService.create(authentication.getName(), request));
    }

    /** 编辑帖子（仅限作者本人）。 */
    @PutMapping("/{id}")
    public Result<PostDetailResponse> update(@PathVariable long id,
                                             @Valid @RequestBody UpdatePostRequest request,
                                             Authentication authentication) {
        return Result.ok(postService.update(id, authentication.getName(), request));
    }

    /** 删除帖子（逻辑删除，仅限作者本人）。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable long id, Authentication authentication) {
        postService.delete(id, authentication.getName());
        return Result.ok();
    }
}
