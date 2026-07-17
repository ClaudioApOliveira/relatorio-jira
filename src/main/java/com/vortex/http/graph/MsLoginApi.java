package com.vortex.http.graph;

import com.vortex.http.graph.dto.DeviceCodeResponse;
import com.vortex.http.graph.dto.GraphTokenResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ms-login")
@Path("/")
public interface MsLoginApi {

    @POST
    @Path("/{tenant}/oauth2/v2.0/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    GraphTokenResponse token(
            @PathParam("tenant") String tenant,
            String formBody
    );

    @POST
    @Path("/{tenant}/oauth2/v2.0/devicecode")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    DeviceCodeResponse deviceCode(
            @PathParam("tenant") String tenant,
            String formBody
    );
}
