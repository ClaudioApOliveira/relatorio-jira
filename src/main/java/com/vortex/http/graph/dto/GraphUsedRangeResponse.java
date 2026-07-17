package com.vortex.http.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphUsedRangeResponse(
        String address,
        Integer rowCount,
        Integer columnCount,
        List<List<String>> text,
        List<List<Object>> values
) {
}
