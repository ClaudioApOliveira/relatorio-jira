package com.vortex.service;

import com.vortex.http.graph.GraphTokenService;
import com.vortex.http.graph.MsGraphApi;
import com.vortex.http.graph.dto.GraphDriveItem;
import com.vortex.http.graph.dto.GraphRangePatchRequest;
import com.vortex.http.graph.dto.GraphUsedRangeResponse;
import com.vortex.http.graph.dto.GraphWorksheetsResponse;
import com.vortex.model.excel.ExcelOnlineTarget;
import com.vortex.model.excel.PlanilhaAtividadeColumns;
import com.vortex.model.excel.PlanilhaAtividadeRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lê/escreve a planilha no Excel Online via Microsoft Graph.
 * Arquivos são resolvidos sempre sob a pasta base (default: VERTEXCODE LTDA).
 */
@ApplicationScoped
public class ExcelOnlineService {

    private static final Logger LOG = Logger.getLogger(ExcelOnlineService.class);

    @Inject
    @RestClient
    MsGraphApi graphApi;

    @Inject
    GraphTokenService tokenService;

    @ConfigProperty(name = "microsoft.excel.drive-id")
    Optional<String> defaultDriveId;

    @ConfigProperty(name = "microsoft.excel.item-id")
    Optional<String> defaultItemId;

    @ConfigProperty(name = "microsoft.excel.worksheet")
    Optional<String> defaultWorksheet;

    @ConfigProperty(name = "microsoft.excel.onedrive-base-folder", defaultValue = "VERTEXCODE LTDA")
    String oneDriveBaseFolder;

    public ExcelOnlineTarget resolveTarget(
            String driveId,
            String itemId,
            String worksheet,
            String fileName,
            String oneDrivePath
    ) {
        String resolvedDrive = blankToNull(driveId);
        String resolvedItem = blankToNull(itemId);
        String resolvedSheet = blankToNull(worksheet);
        String resolvedFileName = blankToNull(fileName);
        String resolvedOneDrivePath = normalizeOneDrivePath(oneDrivePath, oneDriveBaseFolder);

        if (resolvedDrive == null) {
            resolvedDrive = defaultDriveId.filter(s -> !s.isBlank()).orElse(null);
        }
        if (resolvedItem == null) {
            resolvedItem = defaultItemId.filter(s -> !s.isBlank()).orElse(null);
        }
        if (resolvedSheet == null) {
            resolvedSheet = defaultWorksheet.filter(s -> !s.isBlank()).orElse(null);
        }

        if (resolvedOneDrivePath == null && resolvedFileName != null) {
            resolvedOneDrivePath = normalizeOneDrivePath(
                    normalizeExcelFileName(resolvedFileName), oneDriveBaseFolder);
        }

        if ((resolvedDrive == null || resolvedItem == null) && resolvedOneDrivePath != null) {
            GraphDriveItem item = resolveByOneDrivePathOrScan(resolvedOneDrivePath, resolvedFileName);
            resolvedItem = item.id();
            if (item.parentReference() != null && item.parentReference().driveId() != null) {
                resolvedDrive = item.parentReference().driveId();
            }
        }

        if (resolvedItem == null || resolvedItem.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe excelOneDrivePath, excelFileName ou excelItemId "
                            + "(arquivos sempre sob '" + oneDriveBaseFolder + "')");
        }

        if (resolvedDrive == null || resolvedDrive.isBlank()) {
            resolvedDrive = "me";
            LOG.info("Usando /me/drive (OneDrive pessoal)");
        }

        if (resolvedSheet == null || resolvedSheet.isBlank()) {
            LOG.infof("Listando worksheets do item %s ...", resolvedItem);
            resolvedSheet = firstWorksheetName(resolvedDrive, resolvedItem);
            LOG.infof("Worksheet: %s", resolvedSheet);
        }

        return new ExcelOnlineTarget(resolvedDrive, resolvedItem, resolvedSheet);
    }

    public List<PlanilhaAtividadeRow> lerLinhas(ExcelOnlineTarget target) {
        String auth = tokenService.bearerToken();
        LOG.infof("Lendo usedRange da planilha (worksheet=%s)...", target.worksheetName());
        GraphUsedRangeResponse range;
        if (target.isPersonalMeDrive()) {
            range = graphApi.usedRangeMe(
                    auth,
                    target.itemId(),
                    escapeWorksheetName(target.worksheetName()),
                    "address,rowCount,columnCount,text,values"
            );
        } else {
            range = graphApi.usedRange(
                    auth,
                    target.driveId(),
                    target.itemId(),
                    escapeWorksheetName(target.worksheetName()),
                    "address,rowCount,columnCount,text,values"
            );
        }

        List<List<String>> matrix = toStringMatrix(range);
        if (matrix.isEmpty()) {
            return List.of();
        }

        int excelStartRow = parseStartRow(range != null ? range.address() : null);
        List<PlanilhaAtividadeRow> rows = new ArrayList<>();

        for (int i = 0; i < matrix.size(); i++) {
            int excelRowNumber = excelStartRow + i;
            if (excelRowNumber < 4) {
                continue;
            }
            List<String> cells = matrix.get(i);
            if (isEmptyRow(cells)) {
                continue;
            }
            PlanilhaAtividadeRow mapped = mapRow(excelRowNumber, cells);
            if (mapped.isTotalRow()) {
                continue;
            }
            rows.add(mapped);
        }
        return rows;
    }

    public void atualizarDescricoes(ExcelOnlineTarget target, Map<Integer, String> descricaoPorLinhaExcel) {
        if (descricaoPorLinhaExcel == null || descricaoPorLinhaExcel.isEmpty()) {
            LOG.info("Nenhuma célula da coluna I para atualizar (já preenchidas ou sem match).");
            return;
        }
        String auth = tokenService.bearerToken();
        String worksheet = escapeWorksheetName(target.worksheetName());
        LOG.infof("Atualizando %d células na coluna I...", descricaoPorLinhaExcel.size());

        for (Map.Entry<Integer, String> entry : descricaoPorLinhaExcel.entrySet()) {
            int excelRow = entry.getKey();
            if (excelRow < 4) {
                continue;
            }
            String address = "I" + excelRow;
            String value = entry.getValue() != null ? entry.getValue() : "";
            LOG.infof("Excel Online PATCH %s!%s", target.worksheetName(), address);
            if (target.isPersonalMeDrive()) {
                graphApi.patchRangeMe(
                        auth,
                        target.itemId(),
                        worksheet,
                        address,
                        new GraphRangePatchRequest(List.of(List.of(value)))
                );
            } else {
                graphApi.patchRange(
                        auth,
                        target.driveId(),
                        target.itemId(),
                        worksheet,
                        address,
                        new GraphRangePatchRequest(List.of(List.of(value)))
                );
            }
        }
        LOG.info("Atualização da coluna I concluída.");
    }

    private String firstWorksheetName(String driveId, String itemId) {
        GraphWorksheetsResponse response;
        if (driveId == null || driveId.isBlank() || "me".equalsIgnoreCase(driveId)) {
            response = graphApi.listWorksheetsMe(tokenService.bearerToken(), itemId);
        } else {
            response = graphApi.listWorksheets(tokenService.bearerToken(), driveId, itemId);
        }
        if (response == null || response.value() == null || response.value().isEmpty()) {
            throw new IllegalStateException("Nenhuma planilha (worksheet) encontrada no arquivo");
        }
        return response.value().get(0).name();
    }

    private GraphDriveItem resolveByOneDrivePathOrScan(String oneDrivePath, String fileNameHint) {
        try {
            return resolveByOneDrivePath(oneDrivePath);
        } catch (Exception e) {
            String name = normalizeExcelFileName(fileNameHint);
            if (name == null) {
                int slash = oneDrivePath.lastIndexOf('/');
                name = slash >= 0 ? oneDrivePath.substring(slash + 1) : oneDrivePath;
            }
            LOG.warnf(
                    "Caminho '%s' não encontrado (%s); varrendo pasta '%s' por nome '%s'...",
                    oneDrivePath,
                    e.getMessage(),
                    oneDriveBaseFolder,
                    name
            );
            return findFileUnderBaseFolder(tokenService.bearerToken(), name);
        }
    }

    private GraphDriveItem resolveByOneDrivePath(String oneDrivePath) {
        String auth = tokenService.bearerToken();
        LOG.infof("Resolvendo por caminho OneDrive: /%s", oneDrivePath);
        try {
            GraphDriveItem item = graphApi.getByRootPath(
                    auth, oneDrivePath, "id,name,parentReference,folder");
            if (item == null || item.id() == null) {
                throw new IllegalStateException(
                        "Arquivo não encontrado no caminho: " + oneDrivePath);
            }
            if (item.isFolder()) {
                throw new IllegalStateException(
                        "O caminho aponta para uma pasta, não um arquivo: " + oneDrivePath);
            }
            LOG.infof("Arquivo encontrado por caminho: %s (%s)", item.name(), item.id());
            return item;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao abrir '" + oneDrivePath + "' no OneDrive. "
                            + "Confirme pastas e nome do arquivo. Detalhe: " + e.getMessage(),
                    e
            );
        }
    }

    /** Percorre a pasta base e subpastas até achar o nome exato do arquivo. */
    private GraphDriveItem findFileUnderBaseFolder(String auth, String expectedName) {
        String base = blankToNull(oneDriveBaseFolder);
        if (base == null) {
            throw new IllegalStateException("microsoft.excel.onedrive-base-folder não configurada");
        }

        LOG.infof("Buscando '%s' dentro de '/%s' ...", expectedName, base);
        Deque<String> folders = new ArrayDeque<>();
        folders.add(base);
        int visited = 0;
        final int maxFolders = 50;

        while (!folders.isEmpty() && visited < maxFolders) {
            String folderPath = folders.removeFirst();
            visited++;
            try {
                var children = graphApi.listChildrenByPath(
                        auth, folderPath, "id,name,parentReference,folder", 200);
                if (children == null || children.value() == null) {
                    continue;
                }
                for (GraphDriveItem child : children.value()) {
                    if (child == null || child.name() == null) {
                        continue;
                    }
                    if (!child.isFolder() && namesMatch(child.name(), expectedName)) {
                        LOG.infof(
                                "Arquivo encontrado em /%s/%s (%s)",
                                folderPath,
                                child.name(),
                                child.id()
                        );
                        return child;
                    }
                    if (child.isFolder()) {
                        folders.addLast(folderPath + "/" + child.name());
                    }
                }
            } catch (Exception e) {
                LOG.warnf("Falha ao listar '/%s': %s", folderPath, e.getMessage());
            }
        }

        throw new IllegalStateException(
                "Arquivo '" + expectedName + "' não encontrado dentro de '/"
                        + base + "' (pastas percorridas: " + visited + ")."
        );
    }

    /**
     * Normaliza caminho sempre sob a pasta base.
     * Aceita relativo ({@code 2026/07-2026/arquivo.xlsx}) ou já com a base.
     */
    static String normalizeOneDrivePath(String path, String baseFolder) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String base = baseFolder == null ? "" : baseFolder.trim().replace('\\', '/');
        while (base.startsWith("/")) {
            base = base.substring(1);
        }
        while (base.endsWith("/") && base.length() > 1) {
            base = base.substring(0, base.length() - 1);
        }

        String p = path.trim().replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.isBlank()) {
            return null;
        }

        int slash = p.lastIndexOf('/');
        String last = slash >= 0 ? p.substring(slash + 1) : p;
        if (!last.contains(".")) {
            p = p + ".xlsx";
        }

        if (!base.isBlank()) {
            String baseLower = base.toLowerCase();
            if (!p.toLowerCase().startsWith(baseLower + "/")
                    && !p.equalsIgnoreCase(base)) {
                p = base + "/" + p;
            }
        }
        return p;
    }

    static String normalizeExcelFileName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String n = name.trim();
        if (!n.toLowerCase().endsWith(".xlsx")) {
            n = n + ".xlsx";
        }
        return n;
    }

    static boolean namesMatch(String actual, String expectedWithXlsx) {
        if (actual == null || expectedWithXlsx == null) {
            return false;
        }
        String expected = expectedWithXlsx.trim();
        String withoutExt = expected.toLowerCase().endsWith(".xlsx")
                ? expected.substring(0, expected.length() - 5)
                : expected;
        return actual.equalsIgnoreCase(expected) || actual.equalsIgnoreCase(withoutExt);
    }

    static String escapeWorksheetName(String name) {
        if (name == null || name.isBlank()) {
            return "Sheet1";
        }
        return name.replace("'", "''");
    }

    private static int parseStartRow(String address) {
        if (address == null || address.isBlank()) {
            return 1;
        }
        String a = address;
        int bang = a.lastIndexOf('!');
        if (bang >= 0) {
            a = a.substring(bang + 1);
        }
        int i = 0;
        while (i < a.length() && Character.isLetter(a.charAt(i))) {
            i++;
        }
        int j = i;
        while (j < a.length() && Character.isDigit(a.charAt(j))) {
            j++;
        }
        if (j > i) {
            try {
                return Integer.parseInt(a.substring(i, j));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private static List<List<String>> toStringMatrix(GraphUsedRangeResponse range) {
        if (range == null) {
            return List.of();
        }
        if (range.text() != null && !range.text().isEmpty()) {
            return range.text();
        }
        if (range.values() == null) {
            return List.of();
        }
        List<List<String>> out = new ArrayList<>();
        for (List<Object> row : range.values()) {
            List<String> cells = new ArrayList<>();
            if (row != null) {
                for (Object cell : row) {
                    cells.add(cell == null ? "" : String.valueOf(cell).trim());
                }
            }
            out.add(cells);
        }
        return out;
    }

    private static PlanilhaAtividadeRow mapRow(int excelRowNumber, List<String> cells) {
        return new PlanilhaAtividadeRow(
                excelRowNumber,
                cell(cells, PlanilhaAtividadeColumns.NOME),
                cell(cells, PlanilhaAtividadeColumns.DATA),
                cell(cells, PlanilhaAtividadeColumns.DIA_SEMANA),
                cell(cells, PlanilhaAtividadeColumns.INICIO),
                cell(cells, PlanilhaAtividadeColumns.FIM),
                cell(cells, PlanilhaAtividadeColumns.TOTAL),
                cell(cells, PlanilhaAtividadeColumns.TIPO_PROJETO),
                cell(cells, PlanilhaAtividadeColumns.NOME_PROJETO),
                cell(cells, PlanilhaAtividadeColumns.DESCRICAO_ATIVIDADE)
        );
    }

    private static String cell(List<String> cells, int index) {
        if (cells == null || index < 0 || index >= cells.size()) {
            return "";
        }
        String v = cells.get(index);
        return v != null ? v.trim() : "";
    }

    private static boolean isEmptyRow(List<String> cells) {
        if (cells == null || cells.isEmpty()) {
            return true;
        }
        for (String c : cells) {
            if (c != null && !c.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
