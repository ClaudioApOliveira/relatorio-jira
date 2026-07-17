package com.vortex.http.jira;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class JiraAuthHeadersFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "jira.api.email")
    String email;

    @ConfigProperty(name = "jira.api.token")
    String token;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders
    ) {
        String credentials = email + ":" + token;
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        clientOutgoingHeaders.putSingle("Authorization", "Basic " + encoded);
        return clientOutgoingHeaders;
    }
}
