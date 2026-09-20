package com.community.oauth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * GitHub 仓库拉取（公开仓库）。
 * API: https://api.github.com/user/repos?visibility=public&type=owner
 */
@Service
public class GitHubRepoProvider implements RepoProvider {

    private final RestClient restClient = RestClient.create();

    @Override
    public String provider() {
        return "GITHUB";
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RemoteRepo> fetchPublicRepos(String accessToken) {
        try {
            Object[] repos = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.github.com")
                            .path("/user/repos")
                            .queryParam("visibility", "public")
                            .queryParam("type", "owner")
                            .queryParam("per_page", 100)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(Object[].class);
            if (repos == null) {
                return List.of();
            }
            List<RemoteRepo> result = new ArrayList<>();
            for (Object item : repos) {
                Map<String, Object> r = (Map<String, Object>) item;
                result.add(new RemoteRepo(
                        (String) r.get("full_name"),
                        (String) r.get("html_url"),
                        (String) r.get("description"),
                        r.get("stargazers_count") != null ? ((Number) r.get("stargazers_count")).longValue() : 0L));
            }
            return result;
        } catch (Exception e) {
            // T-REPO-1：禁止吞异常返回空列表（会被当成成功 0 仓清快照）
            throw new IllegalStateException("GitHub fetchPublicRepos failed: " + e.getMessage(), e);
        }
    }
}
