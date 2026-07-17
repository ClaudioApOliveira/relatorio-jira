package com.vortex.http.jira.reponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vortex.model.Issue;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse(
        List<Issue> issues,
        String nextPageToken,
        Boolean isLast
) {
    public boolean hasMore() {
        return !Boolean.TRUE.equals(isLast)
                && nextPageToken != null
                && !nextPageToken.isEmpty()
                && issues != null
                && !issues.isEmpty();
    }
}
