package com.community.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程内向量库（测试默认；QDRANT_URL=memory 时启用）。
 */
public class InMemoryRagVectorStore implements RagVectorStore {

    private final Map<String, List<RagPointView>> byParent = new ConcurrentHashMap<>();

    @Override
    public void ensureCollection() {
        // no-op
    }

    @Override
    public synchronized void upsert(List<RagPointView> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        for (RagPointView point : points) {
            String parent = String.valueOf(point.payload().get("parent_content_id"));
            byParent.compute(parent, (k, existing) -> {
                List<RagPointView> list = existing == null ? new CopyOnWriteArrayList<>() : existing;
                list.removeIf(p -> Objects.equals(p.pointId(), point.pointId()));
                list.add(point);
                return list;
            });
        }
    }

    @Override
    public synchronized void deleteByParentContentId(String parentContentId) {
        byParent.remove(parentContentId);
    }

    @Override
    public int countByParentContentId(String parentContentId) {
        List<RagPointView> list = byParent.get(parentContentId);
        return list == null ? 0 : list.size();
    }

    @Override
    public Map<String, Object> findOnePayloadByParent(String parentContentId) {
        List<RagPointView> list = byParent.get(parentContentId);
        if (list == null || list.isEmpty()) {
            return Map.of();
        }
        return list.get(0).payload();
    }

    public synchronized void clear() {
        byParent.clear();
    }
}
