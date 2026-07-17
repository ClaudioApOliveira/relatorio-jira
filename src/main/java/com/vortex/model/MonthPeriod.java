package com.vortex.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MonthPeriod {
    private static final Pattern MONTH_TOKEN =
            Pattern.compile("^(?:(\\d{4})-)?(\\d{1,2})$");

    private MonthPeriod() {
    }

    /**
     * 1º e último dia do mês.
     */
    public static LocalDate startOf(YearMonth ym) {
        return ym.atDay(1);
    }

    public static LocalDate endOf(YearMonth ym) {
        return ym.atEndOfMonth();
    }

    /**
     * Menor início entre os meses.
     */
    public static LocalDate startOf(List<YearMonth> months) {
        return months.stream()
                .map(MonthPeriod::startOf)
                .min(LocalDate::compareTo)
                .orElseThrow();
    }

    /**
     * Maior fim entre os meses.
     */
    public static LocalDate endOf(List<YearMonth> months) {
        return months.stream()
                .map(MonthPeriod::endOf)
                .max(LocalDate::compareTo)
                .orElseThrow();
    }

    /**
     * "06/2026, 07/2026"
     */
    public static String formatLabel(List<YearMonth> months) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT);
        return months.stream().map(fmt::format).collect(Collectors.joining(", "));
    }

    /**
     * Aceita: "2026-06,2026-07" | "6,7" | "2026-07"
     * Se o token for só o mês (ex.: "6"), usa defaultYear.
     */
    public static List<YearMonth> parse(String monthsSpec, Integer defaultYear) {
        int yearFallback = defaultYear != null ? defaultYear : YearMonth.now().getYear();
        if (monthsSpec == null || monthsSpec.isBlank()) {
            return List.of(YearMonth.now());
        }
        LinkedHashSet<YearMonth> seen = new LinkedHashSet<>();
        for (String part : monthsSpec.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            seen.add(parseToken(part, yearFallback));
        }
        if (seen.isEmpty()) {
            return List.of(YearMonth.now());
        }
        return seen.stream().sorted().toList();
    }

    private static YearMonth parseToken(String token, int defaultYear) {
        Matcher m = MONTH_TOKEN.matcher(token);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Mês inválido: '" + token + "'. Use YYYY-MM (ex.: 2026-07) ou M (ex.: 7).");
        }
        int year = m.group(1) != null ? Integer.parseInt(m.group(1)) : defaultYear;
        int month = Integer.parseInt(m.group(2));
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Mês fora do intervalo 1–12: '" + token + "'");
        }
        return YearMonth.of(year, month);
    }

    /**
     * Nome do PDF: tempo-gasto-2026-06_2026-07.pdf
     */
    public static String defaultFileName(List<YearMonth> months) {
        String body = months.stream()
                .map(ym -> String.format("%d-%02d", ym.getYear(), ym.getMonthValue()))
                .collect(Collectors.joining("_"));
        return "tempo-gasto-" + body + ".pdf";
    }
}
