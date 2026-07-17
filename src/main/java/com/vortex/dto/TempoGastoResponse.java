package com.vortex.dto;

import com.vortex.model.WorklogRow;

import java.util.List;
import java.util.Map;

public record TempoGastoResponse(
        String periodLabel,
        String periodStart,
        String periodEnd,
        String authorFilter,
        List<String> projectKeys,
        int worklogCount,
        double totalHours,
        String totalHoursFormatted,
        List<WorklogRow> worklogs,
        /** Linhas da planilha atualizadas (excelRowNumber → descrição). */
        Map<Integer, String> excelUpdatedRows
) {
}
