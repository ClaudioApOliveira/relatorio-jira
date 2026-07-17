package com.vortex.http.jira.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchRequest(
        String jql,
        Integer maxResults,
        List<String> fields,
        String nextPageToken
) {
    public static SearchRequest firstPage(String jql, int maxResults, List<String> fields) {
        return new SearchRequest(jql, maxResults, fields, null);
    }

    public SearchRequest next(String token) {
        return new SearchRequest(jql, maxResults, fields, token);
    }
}
