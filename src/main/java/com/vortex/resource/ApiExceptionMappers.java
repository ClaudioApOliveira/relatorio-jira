package com.vortex.resource;

import com.vortex.dto.ApiErrorBody;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Erros tipados com {@link ApiErrorBody} para o Quarkus indexar o DTO no native.
 */
public class ApiExceptionMappers {

    @ServerExceptionMapper
    public RestResponse<ApiErrorBody> illegalArgument(IllegalArgumentException e) {
        return RestResponse.status(Response.Status.BAD_REQUEST, new ApiErrorBody(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiErrorBody> illegalState(IllegalStateException e) {
        return RestResponse.status(Response.Status.BAD_REQUEST, new ApiErrorBody(e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiErrorBody> unhandled(Exception e) {
        return RestResponse.status(
                Response.Status.INTERNAL_SERVER_ERROR,
                new ApiErrorBody(
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
    }
}
