package com.vortex.service;

import com.vortex.dto.TempoGastoRequest;
import com.vortex.dto.TempoGastoResponse;

public interface TempoGastoReportService {

    TempoGastoResponse generate(TempoGastoRequest request);
}
