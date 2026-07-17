package com.vortex.service.impl;

import com.vortex.dto.TempoGastoRequest;
import com.vortex.dto.TempoGastoResponse;
import com.vortex.model.MonthPeriod;
import com.vortex.model.ReportFormatters;
import com.vortex.model.WorklogRow;
import com.vortex.service.PlanilhaDescricaoSyncService;
import com.vortex.service.TempoGastoReportService;
import com.vortex.service.WorklogCollectorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TempoGastoReportServiceImpl implements TempoGastoReportService {

    @Inject
    WorklogCollectorService collector;

    @Inject
    PlanilhaDescricaoSyncService planilhaSync;

    @Override
    public TempoGastoResponse generate(TempoGastoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body obrigatório: months, projects (ou projectKeys).");
        }

        String monthsSpec = blankToNull(request.months());
        if (monthsSpec == null) {
            throw new IllegalArgumentException(
                    "Campo 'months' obrigatório. Ex.: \"2026-06,2026-07\"");
        }

        List<String> projectKeys = resolveProjects(request);
        if (projectKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Informe 'projects' (ex.: \"IP,PERTI26\") ou 'projectKeys' ([\"IP\",\"PERTI26\"]).");
        }

        List<YearMonth> months = MonthPeriod.parse(monthsSpec, request.year());
        String author = blankToNull(request.author());

        List<WorklogRow> rows = collector.collect(
                months, projectKeys, author, request.limitIssues());

        Map<Integer, String> excelUpdated = Map.of();
        if (Boolean.TRUE.equals(request.excelOnline())) {
            excelUpdated = planilhaSync.sincronizarOnline(
                    blankToNull(request.excelDriveId()),
                    blankToNull(request.excelItemId()),
                    blankToNull(request.excelWorksheet()),
                    blankToNull(request.excelFileName()),
                    blankToNull(request.excelOneDrivePath()),
                    rows
            );
        }

        LocalDate start = MonthPeriod.startOf(months);
        LocalDate end = MonthPeriod.endOf(months);
        double totalHours = rows.stream().mapToDouble(WorklogRow::hours).sum();

        return new TempoGastoResponse(
                MonthPeriod.formatLabel(months),
                start.toString(),
                end.toString(),
                author,
                projectKeys,
                rows.size(),
                totalHours,
                ReportFormatters.formatHours(totalHours),
                rows,
                excelUpdated
        );
    }

    private static List<String> resolveProjects(TempoGastoRequest request) {
        if (request.projectKeys() != null && !request.projectKeys().isEmpty()) {
            return request.projectKeys().stream()
                    .filter(k -> k != null && !k.isBlank())
                    .map(k -> k.trim().toUpperCase())
                    .toList();
        }
        String projects = blankToNull(request.projects());
        if (projects == null) {
            return List.of();
        }
        return Arrays.stream(projects.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
