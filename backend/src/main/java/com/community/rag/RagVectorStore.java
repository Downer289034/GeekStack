package com.community.rag;

import java.util.List;
import java.util.Map;

/**
 * 向量库抽象：内存实现供测试；Qdrant HTTP 供联调/生产。
 */
public interface RagVectorStore {

    void ensureCollection();

    void upsert(List<RagPointView> points);

    void deleteByParentContentId(String parentContentId);

    /** 测试用：按 parent 查已索引点数。 */
    int countByParentContentId(String parentContentId);

    /** 测试用：取一条 payload。 */
    Map<String, Object> findOnePayloadByParent(String parentContentId);

    record RagPointView(String pointId, List<Float> vector, Map<String, Object> payload) {
    }
}
