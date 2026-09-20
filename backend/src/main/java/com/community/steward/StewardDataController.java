package com.community.steward;

import com.community.common.PageResponse;
import com.community.common.Result;
import com.community.steward.dto.CommunityStatsResponse;
import com.community.steward.dto.PenaltyPageResponse;
import com.community.steward.dto.StewardContentItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管家只读数据 API（T-DATA-1）。安全：{@code /api/v1/admin/**} → ADMIN。
 */
@RestController
@RequestMapping("/api/v1/admin/data")
public class StewardDataController {

    private final StewardDataService stewardDataService;

    public StewardDataController(StewardDataService stewardDataService) {
        this.stewardDataService = stewardDataService;
    }

    /** 用户发帖历史。 */
    @GetMapping("/users/{userKey}/posts")
    public Result<PageResponse<StewardContentItem>> userPosts(
            @PathVariable String userKey,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(stewardDataService.pageUserPosts(userKey, current, size));
    }

    /** 用户评论历史。 */
    @GetMapping("/users/{userKey}/comments")
    public Result<PageResponse<StewardContentItem>> userComments(
            @PathVariable String userKey,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(stewardDataService.pageUserComments(userKey, current, size));
    }

    /** 社区聚合统计。 */
    @GetMapping("/stats")
    public Result<CommunityStatsResponse> stats() {
        return Result.ok(stewardDataService.stats());
    }

    /** 判罚回流占位（空列表）。 */
    @GetMapping("/penalties")
    public Result<PenaltyPageResponse> penalties(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(stewardDataService.penalties(current, size));
    }
}
