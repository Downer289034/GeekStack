package com.community.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.oauth.entity.UserRepo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 仓库快照表数据访问（仅 oauth 模块内可使用）。
 */
@Mapper
public interface UserRepoMapper extends BaseMapper<UserRepo> {
}
