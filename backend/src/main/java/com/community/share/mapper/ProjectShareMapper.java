package com.community.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.share.entity.ProjectShare;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目分享表数据访问（仅 share 模块内可使用）。
 */
@Mapper
public interface ProjectShareMapper extends BaseMapper<ProjectShare> {
}
