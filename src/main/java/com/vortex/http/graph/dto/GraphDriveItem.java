package com.vortex.http.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphDriveItem(
        String id,
        String name,
        GraphParentReference parentReference,
        /** Presente quando o item é pasta. */
        GraphFolder folder
) {
    public boolean isFolder() {
        return folder != null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphParentReference(String driveId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphFolder() {
    }
}
