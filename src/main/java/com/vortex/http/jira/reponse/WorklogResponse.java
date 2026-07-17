package com.vortex.http.jira.reponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorklogResponse(
        Integer startAt,
        Integer maxResults,
        Integer total,
        List<Worklog> worklogs
) {
    public List<Worklog> safeWorklogs() {
        return worklogs != null ? worklogs : List.of();
    }

    public boolean hasMore(int currentStartAt) {
        int pageSize = maxResults != null ? maxResults : 100;
        int tot = total != null ? total : 0;
        return !safeWorklogs().isEmpty() && (currentStartAt + pageSize) < tot;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Worklog(
            String id,
            String issueId,
            String started,
            String timeSpent,
            Long timeSpentSeconds,
            Author author
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
            String accountId,
            String displayName,
            String emailAddress
    ) {
        public String resolveName() {
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
            if (emailAddress != null && !emailAddress.isBlank()) {
                return emailAddress;
            }
            return "—";
        }
    }
}
