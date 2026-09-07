package com.community.forum.service;

import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.share.entity.ProjectShare;
import com.community.share.mapper.ProjectShareMapper;
import org.springframework.stereotype.Service;

/**
 * 跨模块支撑：forum → share 仅通过 share 模块的 Mapper 暴露层做存在性校验。
 * 架构说明：理想路径是调用 share 的 Service 接口；此处仅做只读存在性检查，
 * 不写 share 数据，属模块间"只读依赖"，在 M1 阶段保持最小实现（后续可上提为 ShareQueryService）。
 */
@Service
public class ShareExistenceChecker {

    private final ProjectShareMapper shareMapper;

    public ShareExistenceChecker(ProjectShareMapper shareMapper) {
        this.shareMapper = shareMapper;
    }

    /** 断言项目分享存在且未逻辑删除。 */
    public void requireShareExists(long shareId) {
        ProjectShare share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目分享不存在");
        }
    }
}
