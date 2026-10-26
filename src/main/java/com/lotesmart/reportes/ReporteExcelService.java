package com.lotesmart.reportes;

import com.lotesmart.datos.ConsultaDAO;
import com.lotesmart.datos.ReporteDAO;
import com.lotesmart.datos.ResultadoTabla;
import com.lotesmart.modelo.FiltroReporte;
import com.lotesmart.modelo.SesionActual;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReporteExcelService {

    private final ReporteDAO reporteDAO;
    private final ConsultaDAO consultaDAO;
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReporteExcelService() {
        this.reporteDAO = new ReporteDAO();
        this.consultaDAO = new ConsultaDAO();
    }

    public ReporteExcelService(ReporteDAO reporteDAO, ConsultaDAO consultaDAO) {
        this.reporteDAO = reporteDAO;
        this.consultaDAO = consultaDAO;
    }

    /**
     * 3. Reporte de Semáforo de Caducidad y Valor en Riesgo en Excel (.xlsx)
     */
    public void generarReporteSemaforoExcel(String filtroSemaforo, File archivoSalida) throws Exception {
        List<Map<String, Object>> lotes = reporteDAO.obtenerReporteSemaforo(filtroSemaforo);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Semáforo Caducidad");

            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle dataStyle = crearEstiloDatos(wb);
            CellStyle currencyStyle = crearEstiloMoneda(wb);
            CellStyle totalStyle = crearEstiloTotal(wb);

            // Título
            Row rTitulo = sheet.createRow(0);
            Cell cT = rTitulo.createCell(0);
            cT.setCellValue("LOTESMART ENTERPRISE - SEMÁFORO DE CADUCIDAD Y VALOR EN RIESGO");
            cT.setCellStyle(headerStyle);

            Row rSub = sheet.createRow(1);
            rSub.createCell(0).setCellValue("Generado por: " + SesionActual.getInstance().getUsername() +
                    " | " + LocalDateTime.now().format(FECHA_HORA_FMT) + " | Filtro: " + (filtroSemaforo != null ? filtroSemaforo : "TODOS"));

            // Encabezados
            String[] headers = {"ID", "SKU", "Lote", "Producto", "Proveedor", "Caducidad", "Días Rest.", "Disponible", "Costo U.", "Valor Total", "Semáforo", "Estado"};
            Row rHeaders = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell c = rHeaders.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            BigDecimal valorTotal = BigDecimal.ZERO;
            BigDecimal valorRiesgo = BigDecimal.ZERO;
            BigDecimal valorNoVendible = BigDecimal.ZERO;

            for (Map<String, Object> l : lotes) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue((int) l.get("id_lote"));
                r.createCell(1).setCellValue(String.valueOf(l.get("sku")));
                r.createCell(2).setCellValue(String.valueOf(l.get("codigo_lote")));
                r.createCell(3).setCellValue(String.valueOf(l.get("nombre_producto")));
                r.createCell(4).setCellValue(String.valueOf(l.get("nombre_proveedor")));
                r.createCell(5).setCellValue(String.valueOf(l.get("fecha_caducidad")));
                r.createCell(6).setCellValue((int) l.get("dias_restantes"));
                r.createCell(7).setCellValue((int) l.get("cantidad_disponible"));

                Cell cCu = r.createCell(8);
                BigDecimal cu = (BigDecimal) l.get("costo_unitario");
                cCu.setCellValue(cu.doubleValue());
                cCu.setCellStyle(currencyStyle);

                Cell cVt = r.createCell(9);
                BigDecimal vt = (BigDecimal) l.get("valor_existencia");
                cVt.setCellValue(vt.doubleValue());
                cVt.setCellStyle(currencyStyle);

                String sem = String.valueOf(l.get("semaforo"));
                Cell cSem = r.createCell(10);
                cSem.setCellValue(sem);
                cSem.setCellStyle(dataStyle);

                r.createCell(11).setCellValue(String.valueOf(l.get("estado")));

                if ("ROJO".equalsIgnoreCase(sem) || "AMARILLO".equalsIgnoreCase(sem)) {
                    valorRiesgo = valorRiesgo.add(vt);
                } else if ("CADUCADO".equalsIgnoreCase(sem) || "BLOQUEADO".equalsIgnoreCase(sem)) {
                    valorNoVendible = valorNoVendible.add(vt);
                }
                valorTotal = valorTotal.add(vt);
            }

            // Fila de totales
            Row rTotal = sheet.createRow(rowIdx + 1);
            Cell cTotalLabel = rTotal.createCell(7);
            cTotalLabel.setCellValue("VALOR TOTAL:");
            cTotalLabel.setCellStyle(totalStyle);

            Cell cTotalVal = rTotal.createCell(9);
            cTotalVal.setCellValue(valorTotal.doubleValue());
            cTotalVal.setCellStyle(totalStyle);

            Row rRiesgo = sheet.createRow(rowIdx + 2);
            rRiesgo.createCell(7).setCellValue("DINERO EN RIESGO (<60D):");
            Cell cRiesgoVal = rRiesgo.createCell(9);
            cRiesgoVal.setCellValue(valorRiesgo.doubleValue());
            cRiesgoVal.setCellStyle(totalStyle);

            Row rNoVendible = sheet.createRow(rowIdx + 3);
            rNoVendible.createCell(7).setCellValue("VALOR NO VENDIBLE:");
            Cell cNoVendVal = rNoVendible.createCell(9);
            cNoVendVal.setCellValue(valorNoVendible.doubleValue());
            cNoVendVal.setCellStyle(totalStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(archivoSalida)) {
                wb.write(fos);
            }
        }
    }

    /**
     * 4. Reporte Financiero de Ventas y Margen Bruto en Excel (.xlsx)
     */
    public void generarReporteFinancieroExcel(LocalDate inicio, LocalDate fin, File archivoSalida) throws Exception {
        List<Map<String, Object>> ventas = reporteDAO.obtenerReporteFinanciero(inicio, fin);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Financiero Margen Bruto");

            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle currencyStyle = crearEstiloMoneda(wb);
            CellStyle totalStyle = crearEstiloTotal(wb);

            Row rTitulo = sheet.createRow(0);
            rTitulo.createCell(0).setCellValue("LOTESMART ENTERPRISE - REPORTE DE VENTAS Y MARGEN BRUTO");

            Row rSub = sheet.createRow(1);
            rSub.createCell(0).setCellValue("Período: " + (inicio != null ? inicio : "Inicio") + " al " + (fin != null ? fin : "Hoy") +
                    " | Generado: " + LocalDateTime.now().format(FECHA_HORA_FMT));

            String[] headers = {"ID Venta", "Folio", "Fecha / Hora", "Cliente", "Total Venta", "Costo Real Lotes", "Margen Bruto", "% Margen"};
            Row rHeaders = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell c = rHeaders.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            BigDecimal sumaIngresos = BigDecimal.ZERO;
            BigDecimal sumaCostos = BigDecimal.ZERO;
            BigDecimal sumaMargen = BigDecimal.ZERO;

            for (Map<String, Object> v : ventas) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue((int) v.get("id_venta"));
                r.createCell(1).setCellValue(String.valueOf(v.get("folio")));
                r.createCell(2).setCellValue(String.valueOf(v.get("fecha")));
                r.createCell(3).setCellValue(String.valueOf(v.get("cliente")));

                BigDecimal ing = (BigDecimal) v.get("total");
                BigDecimal cos = (BigDecimal) v.get("costo_total");
                BigDecimal mar = (BigDecimal) v.get("margen_bruto");

                Cell cIng = r.createCell(4);
                cIng.setCellValue(ing.doubleValue());
                cIng.setCellStyle(currencyStyle);

                Cell cCos = r.createCell(5);
                cCos.setCellValue(cos.doubleValue());
                cCos.setCellStyle(currencyStyle);

                Cell cMar = r.createCell(6);
                cMar.setCellValue(mar.doubleValue());
                cMar.setCellStyle(currencyStyle);

                double pct = ing.compareTo(BigDecimal.ZERO) > 0 ? (mar.doubleValue() / ing.doubleValue()) * 100.0 : 0.0;
                r.createCell(7).setCellValue(String.format("%.2f%%", pct));

                sumaIngresos = sumaIngresos.add(ing);
                sumaCostos = sumaCostos.add(cos);
                sumaMargen = sumaMargen.add(mar);
            }

            // Totales
            Row rTotal = sheet.createRow(rowIdx + 1);
            rTotal.createCell(3).setCellValue("TOTALES:");

            Cell cTotIng = rTotal.createCell(4);
            cTotIng.setCellValue(sumaIngresos.doubleValue());
            cTotIng.setCellStyle(totalStyle);

            Cell cTotCos = rTotal.createCell(5);
            cTotCos.setCellValue(sumaCostos.doubleValue());
            cTotCos.setCellStyle(totalStyle);

            Cell cTotMar = rTotal.createCell(6);
            cTotMar.setCellValue(sumaMargen.doubleValue());
            cTotMar.setCellStyle(totalStyle);

            double pctGlobal = sumaIngresos.compareTo(BigDecimal.ZERO) > 0
                    ? (sumaMargen.doubleValue() / sumaIngresos.doubleValue()) * 100.0 : 0.0;
            Cell cTotPct = rTotal.createCell(7);
            cTotPct.setCellValue(String.format("%.2f%%", pctGlobal));
            cTotPct.setCellStyle(totalStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(archivoSalida)) {
                wb.write(fos);
            }
        }
    }

    /**
     * 5. Kardex de Movimientos y Bitácora de Auditoría en Excel (.xlsx)
     */
    public void generarKardexYAuditoriaExcel(FiltroReporte filtro, File archivoSalida) throws Exception {
        ResultadoTabla resMovs = consultaDAO.obtenerMovimientos(filtro);
        ResultadoTabla resAuditoria = consultaDAO.obtenerAuditoria(filtro);

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = crearEstiloEncabezado(wb);
            CellStyle dataStyle = crearEstiloDatos(wb);

            // Hoja 1: Kardex de Movimientos
            Sheet sMov = wb.createSheet("Kardex de Movimientos");
            Row rHeaderMov = sMov.createRow(0);
            String[] colsMov = resMovs.getColumnas();
            for (int i = 0; i < colsMov.length; i++) {
                Cell c = rHeaderMov.createCell(i);
                c.setCellValue(colsMov[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Object[] fila : resMovs.getFilas()) {
                Row r = sMov.createRow(rowIdx++);
                for (int j = 0; j < fila.length; j++) {
                    Cell c = r.createCell(j);
                    c.setCellValue(fila[j] != null ? fila[j].toString() : "");
                    c.setCellStyle(dataStyle);
                }
            }
            for (int i = 0; i < colsMov.length; i++) {
                sMov.autoSizeColumn(i);
            }

            // Hoja 2: Bitácora de Auditoría
            Sheet sAud = wb.createSheet("Bitácora de Auditoría");
            Row rHeaderAud = sAud.createRow(0);
            String[] colsAud = resAuditoria.getColumnas();
            for (int i = 0; i < colsAud.length; i++) {
                Cell c = rHeaderAud.createCell(i);
                c.setCellValue(colsAud[i]);
                c.setCellStyle(headerStyle);
            }

            rowIdx = 1;
            for (Object[] fila : resAuditoria.getFilas()) {
                Row r = sAud.createRow(rowIdx++);
                for (int j = 0; j < fila.length; j++) {
                    Cell c = r.createCell(j);
                    c.setCellValue(fila[j] != null ? fila[j].toString() : "");
                    c.setCellStyle(dataStyle);
                }
            }
            for (int i = 0; i < colsAud.length; i++) {
                sAud.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(archivoSalida)) {
                wb.write(fos);
            }
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle crearEstiloDatos(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloMoneda(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloTotal(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
