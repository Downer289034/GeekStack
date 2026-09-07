package com.community.forum.controller;

import com.community.common.Result;
import com.community.forum.dto.CommentResponse;
import com.community.forum.dto.CreateCommentRequest;
import com.community.forum.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口（多态）：GET 公开；发表/删除需认证。targetType 取值 POST | SHARE。
 * 帖子回帖：/api/v1/posts/{postId}/comments；项目分享评论：/api/v1/shares/{shareId}/comments。
 */
@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 回帖树（公开）。 */
    @GetMapping({"/posts/{targetId}/comments", "/shares/{targetId}/comments"})
    public Result<List<CommentResponse>> list(@PathVariable long targetId,
                                              jakarta.servlet.http.HttpServletRequest request) {
        String targetType = request.getRequestURI().contains("/shares/")
                ? CommentService.TARGET_SHARE
                : CommentService.TARGET_POST;
        return Result.ok(commentService.listTree(targetType, targetId));
    }

    /** 发表回帖/评论。 */
    @PostMapping({"/posts/{targetId}/comments", "/shares/{targetId}/comments"})
    public Result<CommentResponse> create(@PathVariable long targetId,
                                          @Valid @RequestBody CreateCommentRequest requestBody,
                                          Authentication authentication,
                                          jakarta.servlet.http.HttpServletRequest request) {
        String targetType = request.getRequestURI().contains("/shares/")
                ? CommentService.TARGET_SHARE
                : CommentService.TARGET_POST;
        return Result.ok(commentService.create(targetType, targetId, authentication.getName(), requestBody));
    }

    /** 删除回帖/评论（仅限作者本人）。 */
    @DeleteMapping({"/posts/{commentId}/comments", "/shares/{commentId}/comments"})
    public Result<Void> delete(@PathVariable long commentId,
                               Authentication authentication,
                               jakarta.servlet.http.HttpServletRequest request) {
        String targetType = request.getRequestURI().contains("/shares/")
                ? CommentService.TARGET_SHARE
                : CommentService.TARGET_POST;
        commentService.delete(targetType, commentId, authentication.getName());
        return Result.ok();
    }
}
