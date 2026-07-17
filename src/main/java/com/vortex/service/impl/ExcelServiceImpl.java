package com.vortex.service.impl;

import com.vortex.model.excel.PlanilhaAtividadeColumns;
import com.vortex.model.excel.PlanilhaAtividadeRow;
import com.vortex.service.ExcelService;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ExcelServiceImpl implements ExcelService {

    @Override
    public List<PlanilhaAtividadeRow> lerLinhas(String caminhoDoArquivo) {
        Path path = Path.of(caminhoDoArquivo);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Arquivo não encontrado: " + caminhoDoArquivo);
        }

        try (FileInputStream fis = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);

            List<PlanilhaAtividadeRow> rows = new ArrayList<>();
            int last = sheet.getLastRowNum();

            for (int i = PlanilhaAtividadeColumns.FIRST_DATA_ROW_INDEX; i <= last; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, formatter, evaluator)) {
                    continue;
                }

                PlanilhaAtividadeRow mapped = mapRow(i + 1, row, formatter, evaluator);
                if (mapped.isTotalRow()) {
                    continue;
                }
                rows.add(mapped);
            }
            return rows;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler Excel: " + caminhoDoArquivo, e);
        }
    }

    @Override
    public void atualizarDescricoes(String caminhoDoArquivo, Map<Integer, String> descricaoPorLinhaExcel) {
        if (descricaoPorLinhaExcel == null || descricaoPorLinhaExcel.isEmpty()) {
            return;
        }

        Path path = Path.of(caminhoDoArquivo);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Arquivo não encontrado: " + caminhoDoArquivo);
        }

        try (FileInputStream fis = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Map.Entry<Integer, String> entry : descricaoPorLinhaExcel.entrySet()) {
                int excelRowNumber = entry.getKey();
                if (excelRowNumber < 4) {
                    continue; // só dados (linha 4+)
                }
                int poiIndex = excelRowNumber - 1;
                Row row = sheet.getRow(poiIndex);
                if (row == null) {
                    row = sheet.createRow(poiIndex);
                }
                Cell cell = row.getCell(PlanilhaAtividadeColumns.DESCRICAO_ATIVIDADE);
                if (cell == null) {
                    cell = row.createCell(PlanilhaAtividadeColumns.DESCRICAO_ATIVIDADE, CellType.STRING);
                }
                String value = entry.getValue() != null ? entry.getValue() : "";
                cell.setCellValue(value);
            }

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                workbook.write(fos);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar coluna I do Excel: " + caminhoDoArquivo, e);
        }
    }

    private static PlanilhaAtividadeRow mapRow(
            int excelRowNumber,
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        return new PlanilhaAtividadeRow(
                excelRowNumber,
                cell(row, PlanilhaAtividadeColumns.NOME, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.DATA, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.DIA_SEMANA, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.INICIO, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.FIM, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.TOTAL, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.TIPO_PROJETO, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.NOME_PROJETO, formatter, evaluator),
                cell(row, PlanilhaAtividadeColumns.DESCRICAO_ATIVIDADE, formatter, evaluator)
        );
    }

    private static String cell(Row row, int col, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        String value = formatter.formatCellValue(cell, evaluator);
        return value != null ? value.trim() : "";
    }

    private static boolean isRowEmpty(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String valor = formatter.formatCellValue(cell, evaluator);
                if (valor != null && !valor.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
