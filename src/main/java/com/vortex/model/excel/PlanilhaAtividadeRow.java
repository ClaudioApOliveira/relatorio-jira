package com.vortex.model.excel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Uma linha de dados da planilha (a partir da linha 4).
 * {@code excelRowNumber} é 1-based (igual ao Excel).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanilhaAtividadeRow(
        int excelRowNumber,
        String nome,
        String data,
        String diaSemana,
        String inicio,
        String fim,
        String total,
        String tipoProjeto,
        String nomeProjeto,
        String descricaoAtividade
) {
    public boolean isTotalRow() {
        return nome != null && nome.toLowerCase().contains("total");
    }

    public PlanilhaAtividadeRow withDescricao(String descricao) {
        return new PlanilhaAtividadeRow(
                excelRowNumber,
                nome,
                data,
                diaSemana,
                inicio,
                fim,
                total,
                tipoProjeto,
                nomeProjeto,
                descricao
        );
    }
}
