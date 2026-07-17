package com.vortex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Campos tipados (sem Map/JsonNode) para Native Image sem registro de reflexão manual.
 * Quarkus indexa este tipo via retorno do Rest Client ({@code JiraApi}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueFields(
        String summary,
        @JsonProperty("customfield_10020") List<Sprint> sprints,
        @JsonProperty("customfield_10016") Double storyPoints
) {
}
