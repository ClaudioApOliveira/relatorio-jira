package com.vortex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorklogRow(
        String author,
        LocalDate workDate,
        String issueKey,
        String summary,
        double hours,
        String timeSpent,
        String sprintName,
        String sprintDates,
        Double storyPoints
) {
}
