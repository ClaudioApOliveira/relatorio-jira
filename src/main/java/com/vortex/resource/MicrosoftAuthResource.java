package com.vortex.resource;

import com.vortex.dto.MicrosoftAuthStatus;
import com.vortex.http.graph.GraphTokenService;
import com.vortex.http.graph.dto.DeviceLoginResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/microsoft")
@Produces(MediaType.APPLICATION_JSON)
public class MicrosoftAuthResource {

    @Inject
    GraphTokenService tokenService;

    /**
     * Inicia login OneDrive pessoal (device code).
     * Abra a URL, digite o código, autorize — o endpoint espera e grava o refresh token.
     * <p>
     * Retorno tipado para indexação Jackson no native.
     */
    @POST
    @Path("/device-login")
    public DeviceLoginResult deviceLogin() {
        return tokenService.loginWithDeviceCode();
    }

    @GET
    @Path("/auth-status")
    public MicrosoftAuthStatus status() {
        return new MicrosoftAuthStatus(
                tokenService.isDelegated(),
                tokenService.hasDelegatedSession()
        );
    }
}
