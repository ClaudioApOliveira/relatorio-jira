package com.vortex.service;

import com.vortex.model.excel.PlanilhaAtividadeRow;

import java.util.List;
import java.util.Map;

public interface ExcelService {

    /** Lê as linhas de dados (a partir da linha 4), ignorando vazias e total. */
    List<PlanilhaAtividadeRow> lerLinhas(String caminhoDoArquivo);

    /**
     * Atualiza a coluna I (Descrição da atividade) das linhas informadas.
     *
     * @param caminhoDoArquivo arquivo .xlsx
     * @param descricaoPorLinhaExcel mapa excelRowNumber (1-based) → texto da coluna I
     */
    void atualizarDescricoes(String caminhoDoArquivo, Map<Integer, String> descricaoPorLinhaExcel);
}
