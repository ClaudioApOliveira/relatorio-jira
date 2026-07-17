package com.vortex.http.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphWorksheetsResponse(List<GraphWorksheet> value) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphWorksheet(String id, String name, Integer position) {
    }
}
