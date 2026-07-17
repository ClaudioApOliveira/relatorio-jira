package com.vortex.model.excel;

/**
 * Índices das colunas da planilha de atividades (cabeçalho na linha 3).
 * Excel 1-based: linha 4 dos dados → índice 3 nas APIs Graph (0-based no usedRange).
 */
public final class PlanilhaAtividadeColumns {

    public static final int FIRST_DATA_ROW_INDEX = 3; // linha 4

    public static final int NOME = 0;                 // A
    public static final int DATA = 1;                 // B
    public static final int DIA_SEMANA = 2;           // C
    public static final int INICIO = 3;               // D
    public static final int FIM = 4;                  // E
    public static final int TOTAL = 5;                // F
    public static final int TIPO_PROJETO = 6;         // G
    public static final int NOME_PROJETO = 7;         // H
    public static final int DESCRICAO_ATIVIDADE = 8;  // I ← coluna editável

    private PlanilhaAtividadeColumns() {
    }
}
