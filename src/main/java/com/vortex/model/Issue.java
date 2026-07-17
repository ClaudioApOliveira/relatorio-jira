package com.vortex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Issue(
        String id,
        String key,
        IssueFields fields
) {
}
