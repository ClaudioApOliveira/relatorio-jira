package com.vortex.model;

public final class ReportFormatters {
    private ReportFormatters() {
    }

    /**
     * 2.5 → "2h 30min" | 2.0 → "2h" | 0.25 → "15min"
     */
    public static String formatHours(double hours) {
        int totalMinutes = (int) Math.round(hours * 60);
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h > 0 && m > 0) return h + "h " + m + "min";
        if (h > 0) return h + "h";
        return m + "min";
    }

    /**
     * null → "—" | 3.0 → "3" | 2.5 → "2.5"
     */
    public static String formatStoryPoints(Double value) {
        if (value == null) return "—";
        if (value == Math.rint(value)) return String.valueOf(value.intValue());
        return String.valueOf(value);
    }
}
