package com.vortex.http.jira;

import com.vortex.http.jira.reponse.SearchResponse;
import com.vortex.http.jira.reponse.WorklogResponse;
import com.vortex.http.jira.request.SearchRequest;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "jira")
@RegisterClientHeaders(JiraAuthHeadersFactory.class)
@ClientHeaderParam(name = "Accept", value = "application/json")
@ClientHeaderParam(name = "Content-Type", value = "application/json")
public interface JiraApi {
    @POST
    @Path("/rest/api/3/search/jql")
    SearchResponse search(SearchRequest body);

    @GET
    @Path("/rest/api/3/issue/{key}/worklog")
    WorklogResponse worklogs(@PathParam("key") String key,
                             @QueryParam("startAt") int startAt,
                             @QueryParam("maxResults") int maxResults);
}
