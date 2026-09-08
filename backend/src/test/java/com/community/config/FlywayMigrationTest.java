package com.community.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 验收标准：flyway migrate 一次通过；所有表含 created_at/updated_at/deleted；关键索引齐全。
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record IndexInfo(String name, boolean unique, String columns) {
    }

    private List<String> tableNames() {
        return jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
    }

    private List<IndexInfo> indexInfos(String table) {
        return jdbcTemplate.query(
                "SELECT index_name, MAX(non_unique) AS non_unique, "
                        + "GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols "
                        + "FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? "
                        + "GROUP BY index_name",
                (rs, rowNum) -> new IndexInfo(rs.getString("index_name"),
                        rs.getInt("non_unique") == 0, rs.getString("cols")),
                table);
    }

    @Test
    void allEightTablesCreatedWithAuditColumns() {
        List<String> tables = tableNames();
        assertThat(tables).containsExactlyInAnyOrder("user", "user_oauth_binding", "user_repo",
                "project_share", "post", "post_category", "comment", "notification",
                "user_like", "user_favorite",
                "flyway_schema_history");

        for (String table : List.of("user", "user_oauth_binding", "user_repo", "project_share",
                "post", "post_category", "comment", "notification", "user_like", "user_favorite")) {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ?",
                    table);
            List<String> names = columns.stream()
                    .map(c -> String.valueOf(c.get("column_name")))
                    .toList();
            assertThat(names).contains("created_at", "updated_at", "deleted");
        }
    }

    @Test
    void categoriesSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM post_category", Integer.class);
        assertThat(count).isEqualTo(4);
        List<String> slugs = jdbcTemplate.queryForList(
                "SELECT slug FROM post_category ORDER BY sort_order", String.class);
        assertThat(slugs).containsExactly("java", "ai", "qa", "resource");
    }

    @Test
    void keyCompositeIndexesExist() {
        assertThat(indexInfos("post"))
                .extracting(IndexInfo::columns)
                .contains("category_id,created_at");
        assertThat(indexInfos("project_share"))
                .extracting(IndexInfo::columns)
                .contains("status,created_at");
        assertThat(indexInfos("comment"))
                .extracting(IndexInfo::columns)
                .contains("target_type,target_id");
    }

    @Test
    void uniqueConstraintsExist() {
        assertThat(indexInfos("user"))
                .filteredOn(IndexInfo::unique)
                .extracting(IndexInfo::columns)
                .contains("username", "email");
        assertThat(indexInfos("user_oauth_binding"))
                .filteredOn(IndexInfo::unique)
                .extracting(IndexInfo::columns)
                .contains("provider,openid");
    }
}
