package com.community.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.forum.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * Post 表数据访问（仅 forum 模块内可使用）。
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
