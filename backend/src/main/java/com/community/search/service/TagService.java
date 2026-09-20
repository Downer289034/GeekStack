package com.community.search.service;

import com.community.search.dto.TagCountResponse;
import com.community.search.mapper.TagCloudMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * G2 / T-BE-4.2 标签云：聚合可见帖子 + PUBLISHED 分享与 Skill 的 tags（同名合并 count）。
 */
@Service
public class TagService {

    private final TagCloudMapper tagCloudMapper;

    public TagService(TagCloudMapper tagCloudMapper) {
        this.tagCloudMapper = tagCloudMapper;
    }

    /** 标签云；无数据返回空列表。 */
    public List<TagCountResponse> listTags() {
        return tagCloudMapper.selectPublishedTagCloud();
    }
}
