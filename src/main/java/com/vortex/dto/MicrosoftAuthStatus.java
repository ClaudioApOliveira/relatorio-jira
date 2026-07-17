package com.vortex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MicrosoftAuthStatus(boolean delegatedMode, boolean hasSession) {
}
