package com.community.oauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.oauth.entity.UserRepo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 仓库快照表数据访问（仅 oauth 模块内可使用）。
 */
@Mapper
public interface UserRepoMapper extends BaseMapper<UserRepo> {

    /**
     * 物理删除快照（T-REPO-1）。
     * 禁止用 {@code delete(...)} 逻辑删：行仍占唯一键，后续 insert 撞键被跳过 → 主页永久空。
     */
    @Delete("DELETE FROM user_repo WHERE user_id = #{userId} AND provider = #{provider}")
    int physicalDeleteByUserAndProvider(@Param("userId") long userId, @Param("provider") String provider);
}
