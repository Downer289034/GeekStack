package com.community.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表数据访问（仅 user 模块内可使用）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
