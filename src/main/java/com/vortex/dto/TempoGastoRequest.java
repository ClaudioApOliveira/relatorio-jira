package com.vortex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Body da requisição.
 *
 * <pre>
 * {
 *   "months": "2026-07",
 *   "projects": "PERTI26",
 *   "author": "Cláudio",
 *   "excelOnline": true,
 *   "excelOneDrivePath": "2026/07-2026/PISOMTECH-PLANILHA-ATIVIDADES-PERBANK-CLAUDIO-07-2026.xlsx"
 * }
 * </pre>
 * Caminhos são sempre resolvidos dentro de {@code VERTEXCODE LTDA}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TempoGastoRequest(
        String months,
        Integer year,
        String projects,
        List<String> projectKeys,
        String author,
        Integer limitIssues,
        Boolean excelOnline,
        String excelDriveId,
        String excelItemId,
        String excelWorksheet,
        /** Nome do arquivo (busca sob VERTEXCODE LTDA). */
        String excelFileName,
        /**
         * Caminho relativo a VERTEXCODE LTDA.
         * Ex.: {@code 2026/07-2026/PISOMTECH-...-07-2026.xlsx}
         */
        String excelOneDrivePath
) {
}
