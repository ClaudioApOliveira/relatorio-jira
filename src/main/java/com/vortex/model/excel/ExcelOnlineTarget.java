package com.vortex.model.excel;

/**
 * Localização da planilha no Excel Online.
 * {@code driveId = "me"} → usa /me/drive/items/... (OneDrive pessoal).
 */
public record ExcelOnlineTarget(
        String driveId,
        String itemId,
        String worksheetName
) {
    public boolean isPersonalMeDrive() {
        return driveId == null || driveId.isBlank() || "me".equalsIgnoreCase(driveId);
    }
}
