package com.vortex.service.impl;

import com.vortex.http.jira.JiraApi;
import com.vortex.http.jira.reponse.SearchResponse;
import com.vortex.http.jira.reponse.WorklogResponse;
import com.vortex.http.jira.request.SearchRequest;
import com.vortex.model.Issue;
import com.vortex.model.IssueFields;
import com.vortex.model.MonthPeriod;
import com.vortex.model.ReportFormatters;
import com.vortex.model.SprintExtractor;
import com.vortex.model.WorklogRow;
import com.vortex.service.WorklogCollectorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class WorklogCollectorServiceImpl implements WorklogCollectorService {

    private static final Logger LOG = Logger.getLogger(WorklogCollectorServiceImpl.class);
    private static final int SEARCH_PAGE_SIZE = 50;
    private static final int WORKLOG_PAGE_SIZE = 100;

    /** IDs fixos — mapeados em {@link IssueFields} via @JsonProperty (native-safe). */
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_SPRINT = "customfield_10020";
    private static final String FIELD_STORY_POINTS = "customfield_10016";

    @Inject
    @RestClient
    JiraApi jiraApi;

    @Override
    public List<WorklogRow> collect(
            List<YearMonth> months,
            List<String> projectKeys,
            String authorFilter,
            Integer limitIssues
    ) {
        LocalDate start = MonthPeriod.startOf(months);
        LocalDate end = MonthPeriod.endOf(months);
        String jql = buildJql(start, end, projectKeys);
        LOG.infof("JQL: %s", jql);
        LOG.infof("Meses: %s", MonthPeriod.formatLabel(months));

        List<String> fields = List.of(FIELD_SUMMARY, FIELD_SPRINT, FIELD_STORY_POINTS);
        List<WorklogRow> rows = new ArrayList<>();
        int seenIssues = 0;
        String nextPageToken = null;

        do {
            SearchRequest request = new SearchRequest(jql, SEARCH_PAGE_SIZE, fields, nextPageToken);
            SearchResponse page = jiraApi.search(request);
            List<Issue> issues = page.issues() != null ? page.issues() : List.of();

            for (Issue issue : issues) {
                if (issue.key() == null || issue.key().isBlank()) {
                    continue;
                }
                seenIssues++;
                if (limitIssues != null && seenIssues > limitIssues) {
                    return sort(rows);
                }

                IssueFields issueFields = issue.fields() != null
                        ? issue.fields()
                        : new IssueFields(null, null, null);
                String summary = issueFields.summary() != null ? issueFields.summary() : "";
                var sprint = SprintExtractor.extract(issueFields.sprints());
                Double storyPoints = issueFields.storyPoints();

                List<WorklogResponse.Worklog> worklogs;
                try {
                    worklogs = listAllWorklogs(issue.key());
                } catch (Exception e) {
                    LOG.warnf(e, "Erro worklog %s", issue.key());
                    continue;
                }

                for (WorklogResponse.Worklog wl : worklogs) {
                    if (wl.started() == null || wl.started().length() < 10) {
                        continue;
                    }
                    LocalDate workDate = LocalDate.parse(wl.started().substring(0, 10));
                    if (workDate.isBefore(start) || workDate.isAfter(end)) {
                        continue;
                    }

                    String author = wl.author() != null ? wl.author().resolveName() : "—";
                    if (authorFilter != null
                            && !authorFilter.isBlank()
                            && !author.toLowerCase(Locale.ROOT)
                            .contains(authorFilter.toLowerCase(Locale.ROOT))) {
                        continue;
                    }

                    long seconds = wl.timeSpentSeconds() != null ? wl.timeSpentSeconds() : 0L;
                    double hours = seconds / 3600.0;
                    String timeSpent = (wl.timeSpent() != null && !wl.timeSpent().isBlank())
                            ? wl.timeSpent()
                            : ReportFormatters.formatHours(hours);

                    rows.add(new WorklogRow(
                            author,
                            workDate,
                            issue.key(),
                            summary,
                            hours,
                            timeSpent,
                            sprint.name(),
                            sprint.dates(),
                            storyPoints
                    ));
                }

                if (seenIssues % 25 == 0) {
                    LOG.infof("Progresso: %d issues, %d worklogs no período", seenIssues, rows.size());
                }
            }

            if (!page.hasMore()) {
                break;
            }
            nextPageToken = page.nextPageToken();
        } while (true);

        LOG.infof("Total: %d issues, %d lançamentos no período", seenIssues, rows.size());
        return sort(rows);
    }

    private List<WorklogResponse.Worklog> listAllWorklogs(String issueKey) {
        List<WorklogResponse.Worklog> out = new ArrayList<>();
        int startAt = 0;
        while (true) {
            WorklogResponse page = jiraApi.worklogs(issueKey, startAt, WORKLOG_PAGE_SIZE);
            List<WorklogResponse.Worklog> batch = page.safeWorklogs();
            out.addAll(batch);

            int total = page.total() != null ? page.total() : 0;
            int maxResults = page.maxResults() != null ? page.maxResults() : WORKLOG_PAGE_SIZE;
            startAt += maxResults;

            if (startAt >= total || batch.isEmpty()) {
                break;
            }
        }
        return out;
    }

    static String buildJql(LocalDate start, LocalDate end, List<String> projectKeys) {
        StringBuilder sb = new StringBuilder();
        sb.append("worklogDate >= \"").append(start).append("\"")
                .append(" AND worklogDate <= \"").append(end).append("\"");
        if (projectKeys != null && !projectKeys.isEmpty()) {
            sb.append(" AND project in (")
                    .append(String.join(", ", projectKeys))
                    .append(")");
        }
        return sb.toString();
    }

    private static List<WorklogRow> sort(List<WorklogRow> rows) {
        rows.sort(Comparator
                .comparing((WorklogRow r) -> r.author().toLowerCase(Locale.ROOT))
                .thenComparing(WorklogRow::workDate)
                .thenComparing(WorklogRow::issueKey));
        return rows;
    }
}
