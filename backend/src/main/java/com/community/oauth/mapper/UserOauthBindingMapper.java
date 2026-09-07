package com.community.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.oauth.entity.UserOauthBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方绑定表数据访问（仅 oauth 模块内可使用）。
 */
@Mapper
public interface UserOauthBindingMapper extends BaseMapper<UserOauthBinding> {
}
