package com.vortex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Sprint(
        Long id,
        String name,
        String state,
        String startDate,
        String endDate
) {
}
