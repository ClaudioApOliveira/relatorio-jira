package com.vortex.resource;

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
            var result = tokenService.loginWithDeviceCode();
            return Response.ok(result).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorBody(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/auth-status")
    public Response status() {
        return Response.ok(new AuthStatus(
                tokenService.isDelegated(),
                tokenService.hasDelegatedSession()
        )).build();
    }

    public record AuthStatus(boolean delegatedMode, boolean hasSession) {
    }

    public record ErrorBody(String message) {
    }
}
