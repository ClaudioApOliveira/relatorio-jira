package com.vortex.service;

import com.vortex.model.WorklogRow;

import java.time.YearMonth;
import java.util.List;

public interface WorklogCollectorService {

    List<WorklogRow> collect(
            List<YearMonth> months,
            List<String> projectKeys,
            String authorFilter,
            Integer limitIssues
    );
}
