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
import jakarta.ws.rs.core.Response;

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
     */
    @POST
    public Response generate(TempoGastoRequest request) {
        try {
            TempoGastoResponse report = reportService.generate(request);
            return Response.ok(report).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorBody(e.getMessage()))
                    .build();
        }
    }

    public record ErrorBody(String message) {
    }
}
