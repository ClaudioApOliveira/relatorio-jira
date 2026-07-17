package com.vortex.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SprintExtractor {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SprintExtractor() {
    }

    public record SprintInfo(String name, String dates) {
    }

    public static SprintInfo extract(List<Sprint> sprints) {
        if (sprints == null || sprints.isEmpty()) {
            return new SprintInfo("", "");
        }

        Sprint chosen = null;
        for (Sprint item : sprints) {
            if (item != null && "active".equalsIgnoreCase(nullToEmpty(item.state()))) {
                chosen = item;
                break;
            }
        }
        if (chosen == null) {
            chosen = sprints.get(sprints.size() - 1);
        }
        if (chosen == null) {
            return new SprintInfo("", "");
        }

        String name = nullToEmpty(chosen.name()).trim();
        return new SprintInfo(name, formatDates(chosen));
    }

    private static String formatDates(Sprint sprint) {
        LocalDate start = parseJiraDate(sprint.startDate());
        LocalDate end = parseJiraDate(sprint.endDate());
        if (start != null && end != null) {
            return start.format(BR) + "–" + end.format(BR);
        }
        if (start != null) {
            return "desde " + start.format(BR);
        }
        if (end != null) {
            return "até " + end.format(BR);
        }
        return "";
    }

    private static LocalDate parseJiraDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
