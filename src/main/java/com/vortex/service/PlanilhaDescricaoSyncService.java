package com.vortex.service;

import com.vortex.model.WorklogRow;
import com.vortex.model.excel.ExcelOnlineTarget;
import com.vortex.model.excel.PlanilhaAtividadeRow;
import com.vortex.model.excel.PlanilhaDescricaoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PlanilhaDescricaoSyncService {

    @Inject
    ExcelService excelService;

    @Inject
    ExcelOnlineService excelOnlineService;

    /** Planilha local (.xlsx no disco). */
    public Map<Integer, String> sincronizar(String caminhoExcel, List<WorklogRow> worklogs) {
        if (caminhoExcel == null || caminhoExcel.isBlank()) {
            throw new IllegalArgumentException("caminhoExcel obrigatório");
        }
        List<PlanilhaAtividadeRow> linhas = excelService.lerLinhas(caminhoExcel);
        Map<Integer, String> updates = PlanilhaDescricaoMapper.buildUpdates(worklogs, linhas);
        excelService.atualizarDescricoes(caminhoExcel, updates);
        return updates;
    }

    /** Planilha no Excel Online (OneDrive via Microsoft Graph). */
    public Map<Integer, String> sincronizarOnline(
            String driveId,
            String itemId,
            String worksheet,
            String fileName,
            String oneDrivePath,
            List<WorklogRow> worklogs
    ) {
        ExcelOnlineTarget target = excelOnlineService.resolveTarget(
                driveId, itemId, worksheet, fileName, oneDrivePath);
        List<PlanilhaAtividadeRow> linhas = excelOnlineService.lerLinhas(target);
        Map<Integer, String> updates = PlanilhaDescricaoMapper.buildUpdates(worklogs, linhas);
        excelOnlineService.atualizarDescricoes(target, updates);
        return updates;
    }
}
