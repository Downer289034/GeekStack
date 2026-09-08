package com.community.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.notify.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知表数据访问（仅 notify 模块内可使用）。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
