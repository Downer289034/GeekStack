package com.community.search.mapper;

import com.community.search.dto.TagCountResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 标签云聚合查询（只读；仅统计 PUBLISHED 且未逻辑删除的分享）。
 */
@Mapper
public interface TagCloudMapper {

    /** 按标签名聚合计数，按 count 降序、name 升序。 */
    @Select("""
            SELECT jt.tag AS name, COUNT(*) AS `count`
            FROM project_share s
            INNER JOIN JSON_TABLE(
                IFNULL(s.tags, '[]'),
                '$[*]' COLUMNS (tag VARCHAR(100) PATH '$')
            ) AS jt
            WHERE s.status = 'PUBLISHED'
              AND s.deleted = 0
              AND jt.tag IS NOT NULL
              AND TRIM(jt.tag) <> ''
            GROUP BY jt.tag
            ORDER BY `count` DESC, name ASC
            """)
    List<TagCountResponse> selectPublishedTagCloud();
}
