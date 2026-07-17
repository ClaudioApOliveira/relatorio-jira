package com.vortex.resource;

import com.vortex.dto.ApiErrorBody;
import com.vortex.dto.MicrosoftAuthStatus;
import com.vortex.http.graph.GraphTokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/microsoft")
@Produces(MediaType.APPLICATION_JSON)
public class MicrosoftAuthResource {

    @Inject
    GraphTokenService tokenService;

    /**
     * Inicia login OneDrive pessoal (device code).
     * Abra a URL, digite o código, autorize — o endpoint espera e grava o refresh token.
     */
    @POST
    @Path("/device-login")
    public Response deviceLogin() {
        try {
            return Response.ok(tokenService.loginWithDeviceCode()).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiErrorBody(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiErrorBody(
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                    .build();
        }
    }

    @GET
    @Path("/auth-status")
    public Response status() {
        return Response.ok(new MicrosoftAuthStatus(
                tokenService.isDelegated(),
                tokenService.hasDelegatedSession()
        )).build();
    }
}
