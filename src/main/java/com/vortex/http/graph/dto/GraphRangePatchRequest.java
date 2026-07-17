package com.vortex.http.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphRangePatchRequest(List<List<String>> values) {
}
