package com.vortex.http.graph;

import com.vortex.http.graph.dto.GraphDriveItem;
import com.vortex.http.graph.dto.GraphRangePatchRequest;
import com.vortex.http.graph.dto.GraphSearchResponse;
import com.vortex.http.graph.dto.GraphUsedRangeResponse;
import com.vortex.http.graph.dto.GraphWorksheetsResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ms-graph")
@Path("/v1.0")
@Produces(MediaType.APPLICATION_JSON)
public interface MsGraphApi {

    /**
     * Arquivo por caminho a partir da raiz do OneDrive.
     * Ex.: VERTEXCODE LTDA/2026/07-2026/arquivo.xlsx
     */
    @GET
    @Path("/me/drive/root:/{path:.+}:")
    GraphDriveItem getByRootPath(
            @HeaderParam("Authorization") String authorization,
            @PathParam("path") String path,
            @QueryParam("$select") String select
    );

    @GET
    @Path("/me/drive/root:/{path:.+}:/children")
    GraphSearchResponse listChildrenByPath(
            @HeaderParam("Authorization") String authorization,
            @PathParam("path") String path,
            @QueryParam("$select") String select,
            @QueryParam("$top") Integer top
    );

    // --- OneDrive pessoal (/me/drive) ---

    @GET
    @Path("/me/drive/items/{itemId}/workbook/worksheets")
    GraphWorksheetsResponse listWorksheetsMe(
            @HeaderParam("Authorization") String authorization,
            @PathParam("itemId") String itemId
    );

    @GET
    @Path("/me/drive/items/{itemId}/workbook/worksheets('{worksheet}')/usedRange")
    GraphUsedRangeResponse usedRangeMe(
            @HeaderParam("Authorization") String authorization,
            @PathParam("itemId") String itemId,
            @PathParam("worksheet") String worksheet,
            @QueryParam("$select") String select
    );

    @PATCH
    @Path("/me/drive/items/{itemId}/workbook/worksheets('{worksheet}')/range(address='{address}')")
    void patchRangeMe(
            @HeaderParam("Authorization") String authorization,
            @PathParam("itemId") String itemId,
            @PathParam("worksheet") String worksheet,
            @PathParam("address") String address,
            GraphRangePatchRequest body
    );

    // --- Drive explícito (GUID retornado pelo Graph) ---

    @GET
    @Path("/drives/{driveId}/items/{itemId}/workbook/worksheets")
    GraphWorksheetsResponse listWorksheets(
            @HeaderParam("Authorization") String authorization,
            @PathParam("driveId") String driveId,
            @PathParam("itemId") String itemId
    );

    @GET
    @Path("/drives/{driveId}/items/{itemId}/workbook/worksheets('{worksheet}')/usedRange")
    GraphUsedRangeResponse usedRange(
            @HeaderParam("Authorization") String authorization,
            @PathParam("driveId") String driveId,
            @PathParam("itemId") String itemId,
            @PathParam("worksheet") String worksheet,
            @QueryParam("$select") String select
    );

    @PATCH
    @Path("/drives/{driveId}/items/{itemId}/workbook/worksheets('{worksheet}')/range(address='{address}')")
    void patchRange(
            @HeaderParam("Authorization") String authorization,
            @PathParam("driveId") String driveId,
            @PathParam("itemId") String itemId,
            @PathParam("worksheet") String worksheet,
            @PathParam("address") String address,
            GraphRangePatchRequest body
    );
}
