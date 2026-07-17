package com.vortex.resource;

import com.vortex.dto.TempoGastoRequest;
import com.vortex.dto.TempoGastoResponse;
import com.vortex.service.TempoGastoReportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/reports/tempo-gasto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TempoGastoResource {

    @Inject
    TempoGastoReportService reportService;

    /**
     * Coleta worklogs do Jira e, com {@code excelOnline: true}, preenche a coluna I
     * da planilha no OneDrive (só onde Descrição estiver vazia).
     * Login Microsoft: {@code POST /api/microsoft/device-login}.
     * <p>
     * Retorno tipado (não {@code Response}) para o Quarkus indexar o DTO no native.
     */
    @POST
    public TempoGastoResponse generate(TempoGastoRequest request) {
        return reportService.generate(request);
    }
}
