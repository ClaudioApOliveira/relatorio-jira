package com.vortex.model.excel;

import com.vortex.model.WorklogRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cruza worklogs (por {@code workDate}) com linhas da planilha.
 * <ul>
 *   <li>Só preenche coluna I se estiver vazia</li>
 *   <li>{@code hours == 8} → preenche manhã e tarde com a mesma descrição (dia inteiro)</li>
 *   <li>Caso contrário: 1ª atividade → manhã; demais → tarde (juntas com "; ")</li>
 *   <li>Ignora linhas com Total 0:00:00 (fim de semana)</li>
 * </ul>
 */
public final class PlanilhaDescricaoMapper {

    private static final double FULL_DAY_HOURS = 8.0;
    private static final double FULL_DAY_EPSILON = 0.05;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    private PlanilhaDescricaoMapper() {
    }

    public static Map<Integer, String> buildUpdates(
            List<WorklogRow> worklogs,
            List<PlanilhaAtividadeRow> planilhaRows
    ) {
        Map<LocalDate, List<WorklogRow>> byDate = groupWorklogs(worklogs);
        Map<LocalDate, List<PlanilhaAtividadeRow>> rowsByDate = groupPlanilha(planilhaRows);

        Map<Integer, String> updates = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, List<WorklogRow>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorklogRow> dayLogs = entry.getValue();
            List<PlanilhaAtividadeRow> dayRows = rowsByDate.getOrDefault(date, List.of());
            if (dayRows.isEmpty() || dayLogs.isEmpty()) {
                continue;
            }

            PlanilhaAtividadeRow morning = findMorning(dayRows);
            PlanilhaAtividadeRow afternoon = findAfternoon(dayRows);

            List<WorklogRow> pending = new ArrayList<>(dayLogs);

            // hours == 8 → dia inteiro: mesma descrição nos dois slots
            List<WorklogRow> fullDayLogs = pending.stream()
                    .filter(w -> isFullDayHours(w.hours()))
                    .toList();
            if (!fullDayLogs.isEmpty()) {
                String text = formatMany(fullDayLogs);
                if (canFill(morning)) {
                    updates.put(morning.excelRowNumber(), text);
                }
                if (canFill(afternoon)) {
                    updates.put(afternoon.excelRowNumber(), text);
                }
                pending.removeIf(w -> isFullDayHours(w.hours()));
                if (!pending.isEmpty() && afternoon != null) {
                    if (canFill(afternoon) && !updates.containsKey(afternoon.excelRowNumber())) {
                        updates.put(afternoon.excelRowNumber(), formatMany(pending));
                    } else if (updates.containsKey(afternoon.excelRowNumber())) {
                        updates.put(
                                afternoon.excelRowNumber(),
                                updates.get(afternoon.excelRowNumber()) + "; " + formatMany(pending)
                        );
                    }
                }
                continue;
            }

            if (canFill(morning) && !pending.isEmpty()) {
                WorklogRow first = pending.remove(0);
                updates.put(morning.excelRowNumber(), formatOne(first));
            }

            if (canFill(afternoon) && !pending.isEmpty()) {
                updates.put(afternoon.excelRowNumber(), formatMany(pending));
            }
        }

        return updates;
    }

    static boolean canFill(PlanilhaAtividadeRow row) {
        return row != null
                && isBlank(row.descricaoAtividade())
                && hasWorkedHours(row.total());
    }

    /** 8h (ou mais) = dia inteiro na planilha. */
    static boolean isFullDayHours(double hours) {
        return Math.abs(hours - FULL_DAY_HOURS) < FULL_DAY_EPSILON || hours >= FULL_DAY_HOURS;
    }

    static String formatOne(WorklogRow wl) {
        String key = wl.issueKey() != null ? wl.issueKey().trim() : "";
        String summary = wl.summary() != null ? wl.summary().trim() : "";
        if (key.isEmpty()) {
            return summary;
        }
        if (summary.isEmpty()) {
            return key;
        }
        return key + " - " + summary;
    }

    static String formatMany(List<WorklogRow> worklogs) {
        return worklogs.stream()
                .map(PlanilhaDescricaoMapper::formatOne)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("; "));
    }

    private static Map<LocalDate, List<WorklogRow>> groupWorklogs(List<WorklogRow> worklogs) {
        Map<LocalDate, List<WorklogRow>> map = new HashMap<>();
        if (worklogs == null) {
            return map;
        }
        for (WorklogRow wl : worklogs) {
            if (wl == null || wl.workDate() == null) {
                continue;
            }
            map.computeIfAbsent(wl.workDate(), d -> new ArrayList<>()).add(wl);
        }
        for (List<WorklogRow> list : map.values()) {
            list.sort(Comparator
                    .comparing(WorklogRow::issueKey, Comparator.nullsLast(String::compareTo))
                    .thenComparing(WorklogRow::summary, Comparator.nullsLast(String::compareTo)));
        }
        return map;
    }

    private static Map<LocalDate, List<PlanilhaAtividadeRow>> groupPlanilha(
            List<PlanilhaAtividadeRow> rows
    ) {
        Map<LocalDate, List<PlanilhaAtividadeRow>> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (PlanilhaAtividadeRow row : rows) {
            if (row == null || row.isTotalRow()) {
                continue;
            }
            LocalDate date = parseExcelDate(row.data());
            if (date == null) {
                continue;
            }
            map.computeIfAbsent(date, d -> new ArrayList<>()).add(row);
        }
        return map;
    }

    static LocalDate parseExcelDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    static PlanilhaAtividadeRow findMorning(List<PlanilhaAtividadeRow> dayRows) {
        return dayRows.stream()
                .filter(PlanilhaDescricaoMapper::isMorningSlot)
                .findFirst()
                .orElse(null);
    }

    static PlanilhaAtividadeRow findAfternoon(List<PlanilhaAtividadeRow> dayRows) {
        return dayRows.stream()
                .filter(PlanilhaDescricaoMapper::isAfternoonSlot)
                .findFirst()
                .orElse(null);
    }

    static boolean isMorningSlot(PlanilhaAtividadeRow row) {
        return startsWithHour(row.inicio(), 9) || matchesTotalHours(row.total(), 3);
    }

    static boolean isAfternoonSlot(PlanilhaAtividadeRow row) {
        return startsWithHour(row.inicio(), 13) || matchesTotalHours(row.total(), 5);
    }

    static boolean hasWorkedHours(String total) {
        if (isBlank(total)) {
            return false;
        }
        String t = total.trim();
        return !(t.startsWith("0:00") || t.equals("0") || t.equals("00:00:00"));
    }

    private static boolean startsWithHour(String inicio, int hour) {
        if (isBlank(inicio)) {
            return false;
        }
        String v = inicio.trim().toLowerCase(Locale.ROOT);
        return v.startsWith(hour + ":") || v.startsWith(String.format("%02d:", hour));
    }

    private static boolean matchesTotalHours(String total, int hours) {
        if (isBlank(total)) {
            return false;
        }
        String t = total.trim();
        return t.startsWith(hours + ":") || t.startsWith(String.format("%02d:", hours));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
