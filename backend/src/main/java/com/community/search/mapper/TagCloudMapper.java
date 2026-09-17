package com.community.search.mapper;

import com.community.search.dto.TagCountResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 标签云聚合：PUBLISHED 的项目分享 + Skill（UNION）。
 */
@Mapper
public interface TagCloudMapper {

    /** 按标签名聚合计数，按 count 降序、name 升序。 */
    @Select("""
            SELECT name, SUM(cnt) AS `count`
            FROM (
              SELECT jt.tag AS name, COUNT(*) AS cnt
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
              UNION ALL
              SELECT jt.tag AS name, COUNT(*) AS cnt
              FROM skill sk
              INNER JOIN JSON_TABLE(
                  IFNULL(sk.tags, '[]'),
                  '$[*]' COLUMNS (tag VARCHAR(100) PATH '$')
              ) AS jt
              WHERE sk.status = 'PUBLISHED'
                AND sk.deleted = 0
                AND jt.tag IS NOT NULL
                AND TRIM(jt.tag) <> ''
              GROUP BY jt.tag
            ) t
            GROUP BY name
            ORDER BY `count` DESC, name ASC
            """)
    List<TagCountResponse> selectPublishedTagCloud();
}
