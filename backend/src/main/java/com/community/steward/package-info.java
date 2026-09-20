/**
 * 管家只读数据面（T-DATA-1 一期）：用户发帖/评论历史、社区统计、判罚回流占位。
 *
 * <p>路径挂在 {@code /api/v1/admin/data/**}，鉴权复用 ADMIN（含 steward_bot）。
 * 模块间只走各域 Service，不跨包 import Mapper。口径见 {@code docs/T-DATA-1-一期草案.md}。
 */
package com.community.steward;
