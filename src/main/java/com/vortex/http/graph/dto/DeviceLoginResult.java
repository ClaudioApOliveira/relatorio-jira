package com.vortex.http.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceLoginResult(
        boolean success,
        String instructions,
        String userCode,
        String verificationUri,
        String status
) {
}
