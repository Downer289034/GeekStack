package com.community.rag;

import java.util.List;

/**
 * Embedding 客户端抽象（测试可 mock）。
 */
public interface EmbeddingClient {

    List<List<Float>> embed(List<String> texts);
}
