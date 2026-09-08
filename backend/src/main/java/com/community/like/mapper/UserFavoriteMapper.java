package com.community.like.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.like.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserFavorite 表数据访问（仅 like 模块内可使用）。
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
