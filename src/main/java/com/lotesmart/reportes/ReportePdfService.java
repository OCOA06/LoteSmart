package com.lotesmart.reportes;

import com.lotesmart.datos.ReporteDAO;
import com.lotesmart.modelo.SesionActual;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportePdfService {

    private final ReporteDAO reporteDAO;
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Font FONT_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(24, 43, 73));
    private static final Font FONT_SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(50, 70, 90));
    private static final Font FONT_TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font FONT_TEXTO_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font FONT_TH = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONT_TOTAL = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 80, 160));

    public ReportePdfService() {
        this.reporteDAO = new ReporteDAO();
    }

    public ReportePdfService(ReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    /**
     * 1. Ticket / Nota de Venta FEFO en PDF
     */
    public void generarTicketVentaPdf(int idVenta, File archivoSalida) throws Exception {
        Map<String, Object> data = reporteDAO.obtenerDatosTicketVenta(idVenta);
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No se encontró información para la venta ID " + idVenta);
        }

        Document doc = new Document(PageSize.LETTER, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(archivoSalida));
        doc.open();

        // Encabezado institucional
        doc.add(crearEncabezadoInstitucional("NOTA DE VENTA Y CERTIFICADO DE LOTES FEFO", "Folio: " + data.get("folio")));

        // Datos del cliente y venta
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(15);

        infoTable.addCell(crearCeldaInfo("Cliente: " + data.get("cliente_nombre") + "\nRFC: " + data.get("cliente_rfc") +
                "\nTeléfono: " + data.get("cliente_telefono") + "\nCorreo: " + data.get("cliente_correo")));
        infoTable.addCell(crearCeldaInfo("Fecha Operación: " + data.get("fecha") +
                "\nAtendido por: " + data.get("vendedor_nombre") + " (" + data.get("vendedor_usuario") + ")" +
                "\nEstado: " + data.get("estado") +
                (data.get("motivo_cancelacion") != null ? "\nMotivo Canc.: " + data.get("motivo_cancelacion") : "")));

        doc.add(infoTable);

        // Tabla de partidas con detalle de lote
        PdfPTable tablaPartidas = new PdfPTable(new float[]{1.5f, 3.5f, 1.5f, 1.2f, 1.5f, 1.5f});
        tablaPartidas.setWidthPercentage(100);

        String[] headers = {"SKU", "Producto", "Lote Surtido", "Caducidad", "Cant. (Uds)", "Importe"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_TH));
            c.setBackgroundColor(new Color(24, 60, 110));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(6);
            tablaPartidas.addCell(c);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partidas = (List<Map<String, Object>>) data.get("partidas");
        int totalUds = 0;
        if (partidas != null) {
            for (Map<String, Object> p : partidas) {
                tablaPartidas.addCell(crearCeldaTexto(String.valueOf(p.get("sku")), Element.ALIGN_CENTER));
                tablaPartidas.addCell(crearCeldaTexto(String.valueOf(p.get("producto")), Element.ALIGN_LEFT));
                tablaPartidas.addCell(crearCeldaTexto(String.valueOf(p.get("codigo_lote")), Element.ALIGN_CENTER));
                tablaPartidas.addCell(crearCeldaTexto(String.valueOf(p.get("fecha_caducidad")), Element.ALIGN_CENTER));
                int cant = (int) p.get("cantidad");
                totalUds += cant;
                tablaPartidas.addCell(crearCeldaTexto(String.valueOf(cant), Element.ALIGN_RIGHT));
                BigDecimal sub = (BigDecimal) p.get("subtotal");
                tablaPartidas.addCell(crearCeldaTexto(String.format("$%.2f", sub), Element.ALIGN_RIGHT));
            }
        }
        doc.add(tablaPartidas);

        // Totales y certificación
        Paragraph pTotal = new Paragraph();
        pTotal.setSpacingBefore(12);
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        BigDecimal total = (BigDecimal) data.get("total");
        pTotal.add(new Phrase("Total Unidades: " + totalUds + " | GRAN TOTAL: " + String.format("$%.2f", total), FONT_TOTAL));
        doc.add(pTotal);

        Paragraph pCert = new Paragraph();
        pCert.setSpacingBefore(20);
        pCert.add(new Phrase("Certificación de Trazabilidad Sanitaria FEFO:\n" +
                "Los productos amparados en este documento han sido despachados bajo estricto control de caducidad First Expired, First Out. " +
                "Garantía de frescura y control de retiro sanitario trazable en base de datos PostgreSQL.", FONT_TEXTO));
        doc.add(pCert);

        doc.close();
    }

    /**
     * 2. Alerta Sanitaria de Trazabilidad / Retiro de Lote en PDF
     */
    public void generarAlertaSanitariaPdf(String codigoLote, File archivoSalida) throws Exception {
        Map<String, Object> data = reporteDAO.obtenerDatosRetiroLote(codigoLote);
        if (data == null || !data.containsKey("id_lote")) {
            throw new IllegalArgumentException("No se encontró información para el lote: " + codigoLote);
        }

        Document doc = new Document(PageSize.LETTER, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(archivoSalida));
        doc.open();

        doc.add(crearEncabezadoInstitucional("ORDEN DE RETIRO SANITARIO Y TRAZABILIDAD", "LOTE OBJETO DE ALERTA: " + codigoLote));

        // Resumen técnico del lote
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(15);

        infoTable.addCell(crearCeldaInfo("Producto: " + data.get("producto_nombre") + " [" + data.get("sku") + "]" +
                "\nProveedor Origen: " + data.get("proveedor_nombre") +
                "\nFecha Caducidad: " + data.get("fecha_caducidad") + " (" + data.get("dias_restantes") + " días rest.)"));
        infoTable.addCell(crearCeldaInfo("Cantidad Inicial: " + data.get("cantidad_inicial") +
                "\nExistencia en Almacén: " + data.get("cantidad_disponible") +
                "\nUnidades en Mercado (Distribuidas): " + data.get("total_unidades_distribuidas") +
                "\nEstado Actual: " + data.get("estado") +
                (data.get("motivo_bloqueo") != null ? "\nMotivo Bloqueo: " + data.get("motivo_bloqueo") : "")));

        doc.add(infoTable);

        Paragraph pAviso = new Paragraph("CLIENTES Y DESTINATARIOS AFECTADOS PARA RECOLECCIÓN INMEDIATA:", FONT_SUBTITULO);
        pAviso.setSpacingAfter(8);
        doc.add(pAviso);

        // Tabla de clientes afectados
        PdfPTable tablaClientes = new PdfPTable(new float[]{1.2f, 1.8f, 3.0f, 1.5f, 2.0f, 1.0f});
        tablaClientes.setWidthPercentage(100);

        String[] headers = {"Folio Venta", "Fecha Entrega", "Cliente", "Teléfono", "Correo", "Uds."};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_TH));
            c.setBackgroundColor(new Color(180, 40, 40)); // Rojo alerta
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(5);
            tablaClientes.addCell(c);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clientes = (List<Map<String, Object>>) data.get("clientes_afectados");
        if (clientes != null && !clientes.isEmpty()) {
            for (Map<String, Object> c : clientes) {
                tablaClientes.addCell(crearCeldaTexto(String.valueOf(c.get("folio_venta")), Element.ALIGN_CENTER));
                tablaClientes.addCell(crearCeldaTexto(String.valueOf(c.get("fecha_venta")), Element.ALIGN_CENTER));
                tablaClientes.addCell(crearCeldaTexto(String.valueOf(c.get("nombre_cliente")), Element.ALIGN_LEFT));
                tablaClientes.addCell(crearCeldaTexto(String.valueOf(c.get("telefono_cliente")), Element.ALIGN_CENTER));
                tablaClientes.addCell(crearCeldaTexto(String.valueOf(c.get("correo_cliente")), Element.ALIGN_LEFT));
                tablaClientes.addCell(crearCeldaTexto(String.valueOf(c.get("cantidad_entregada")), Element.ALIGN_RIGHT));
            }
        } else {
            PdfPCell vacio = new PdfPCell(new Phrase("No se registran despachos de este lote a clientes. Las existencias permanecen íntegras en almacén.", FONT_TEXTO_BOLD));
            vacio.setColspan(6);
            vacio.setPadding(10);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            tablaClientes.addCell(vacio);
        }
        doc.add(tablaClientes);

        doc.close();
    }

    /**
     * 3. Reporte de Semáforo de Caducidad y Valor en Riesgo en PDF
     */
    public void generarReporteSemaforoPdf(String filtroSemaforo, File archivoSalida) throws Exception {
        List<Map<String, Object>> lotes = reporteDAO.obtenerReporteSemaforo(filtroSemaforo);

        Document doc = new Document(PageSize.LETTER.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(archivoSalida));
        doc.open();

        doc.add(crearEncabezadoInstitucional("REPORTE DE SEMÁFORO DE CADUCIDAD Y VALOR EN RIESGO",
                "Filtro aplicado: " + (filtroSemaforo != null ? filtroSemaforo : "TODOS")));

        PdfPTable tabla = new PdfPTable(new float[]{1.0f, 1.2f, 2.5f, 2.0f, 1.2f, 0.8f, 1.0f, 1.0f, 1.2f, 1.0f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(12);

        String[] headers = {"SKU", "Lote", "Producto", "Proveedor", "Caducidad", "Días", "Disp.", "Costo U.", "Valor Total", "Semáforo"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_TH));
            c.setBackgroundColor(new Color(24, 60, 110));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(5);
            tabla.addCell(c);
        }

        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal valorRiesgo = BigDecimal.ZERO;
        BigDecimal valorNoVendible = BigDecimal.ZERO;

        for (Map<String, Object> l : lotes) {
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("sku")), Element.ALIGN_CENTER));
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("codigo_lote")), Element.ALIGN_CENTER));
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("nombre_producto")), Element.ALIGN_LEFT));
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("nombre_proveedor")), Element.ALIGN_LEFT));
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("fecha_caducidad")), Element.ALIGN_CENTER));
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("dias_restantes")), Element.ALIGN_RIGHT));
            tabla.addCell(crearCeldaTexto(String.valueOf(l.get("cantidad_disponible")), Element.ALIGN_RIGHT));
            BigDecimal cu = (BigDecimal) l.get("costo_unitario");
            tabla.addCell(crearCeldaTexto(String.format("$%.2f", cu), Element.ALIGN_RIGHT));
            BigDecimal vt = (BigDecimal) l.get("valor_existencia");
            tabla.addCell(crearCeldaTexto(String.format("$%.2f", vt), Element.ALIGN_RIGHT));

            String sem = String.valueOf(l.get("semaforo"));
            PdfPCell cSem = crearCeldaTexto(sem, Element.ALIGN_CENTER);
            if ("ROJO".equalsIgnoreCase(sem)) {
                cSem.setBackgroundColor(new Color(255, 204, 204));
                valorRiesgo = valorRiesgo.add(vt);
            } else if ("AMARILLO".equalsIgnoreCase(sem)) {
                cSem.setBackgroundColor(new Color(255, 245, 180));
                valorRiesgo = valorRiesgo.add(vt);
            } else if ("VERDE".equalsIgnoreCase(sem)) {
                cSem.setBackgroundColor(new Color(210, 245, 210));
            } else if ("CADUCADO".equalsIgnoreCase(sem) || "BLOQUEADO".equalsIgnoreCase(sem)) {
                cSem.setBackgroundColor(new Color(230, 210, 240));
                valorNoVendible = valorNoVendible.add(vt);
            }
            tabla.addCell(cSem);

            valorTotal = valorTotal.add(vt);
        }

        doc.add(tabla);

        // Resumen financiero
        Paragraph pResumen = new Paragraph();
        pResumen.setSpacingBefore(12);
        pResumen.add(new Phrase("Valor Total Inventario: " + String.format("$%.2f", valorTotal) +
                "  |  Dinero en Riesgo (<60d): " + String.format("$%.2f", valorRiesgo) +
                "  |  Valor No Vendible (Caducado/Bloq): " + String.format("$%.2f", valorNoVendible), FONT_TOTAL));
        doc.add(pResumen);

        doc.close();
    }

    /**
     * 4. Reporte Financiero de Ventas y Margen Bruto en PDF
     */
    public void generarReporteFinancieroPdf(LocalDate inicio, LocalDate fin, File archivoSalida) throws Exception {
        List<Map<String, Object>> ventas = reporteDAO.obtenerReporteFinanciero(inicio, fin);

        Document doc = new Document(PageSize.LETTER, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(archivoSalida));
        doc.open();

        String rangoFechas = (inicio != null ? inicio.toString() : "Inicio") + " al " + (fin != null ? fin.toString() : "Hoy");
        doc.add(crearEncabezadoInstitucional("REPORTE FINANCIERO DE VENTAS Y MARGEN BRUTO", "Período: " + rangoFechas));

        PdfPTable tabla = new PdfPTable(new float[]{1.2f, 1.8f, 2.5f, 1.5f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(12);

        String[] headers = {"Folio", "Fecha / Hora", "Cliente", "Ingreso Total", "Costo Real Lotes", "Margen Bruto"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_TH));
            c.setBackgroundColor(new Color(24, 60, 110));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(6);
            tabla.addCell(c);
        }

        BigDecimal sumaIngresos = BigDecimal.ZERO;
        BigDecimal sumaCostos = BigDecimal.ZERO;
        BigDecimal sumaMargen = BigDecimal.ZERO;

        for (Map<String, Object> v : ventas) {
            tabla.addCell(crearCeldaTexto(String.valueOf(v.get("folio")), Element.ALIGN_CENTER));
            tabla.addCell(crearCeldaTexto(String.valueOf(v.get("fecha")), Element.ALIGN_CENTER));
            tabla.addCell(crearCeldaTexto(String.valueOf(v.get("cliente")), Element.ALIGN_LEFT));

            BigDecimal ing = (BigDecimal) v.get("total");
            BigDecimal cos = (BigDecimal) v.get("costo_total");
            BigDecimal mar = (BigDecimal) v.get("margen_bruto");

            tabla.addCell(crearCeldaTexto(String.format("$%.2f", ing), Element.ALIGN_RIGHT));
            tabla.addCell(crearCeldaTexto(String.format("$%.2f", cos), Element.ALIGN_RIGHT));
            tabla.addCell(crearCeldaTexto(String.format("$%.2f", mar), Element.ALIGN_RIGHT));

            sumaIngresos = sumaIngresos.add(ing);
            sumaCostos = sumaCostos.add(cos);
            sumaMargen = sumaMargen.add(mar);
        }

        doc.add(tabla);

        double pctMargen = sumaIngresos.compareTo(BigDecimal.ZERO) > 0
                ? (sumaMargen.doubleValue() / sumaIngresos.doubleValue()) * 100.0
                : 0.0;

        Paragraph pTot = new Paragraph();
        pTot.setSpacingBefore(15);
        pTot.setAlignment(Element.ALIGN_RIGHT);
        pTot.add(new Phrase("Ingresos: " + String.format("$%.2f", sumaIngresos) +
                "  |  Costo Mercancía: " + String.format("$%.2f", sumaCostos) +
                "  |  Margen Bruto: " + String.format("$%.2f", sumaMargen) +
                String.format(" (%.1f%%)", pctMargen), FONT_TOTAL));
        doc.add(pTot);

        doc.close();
    }

    private PdfPTable crearEncabezadoInstitucional(String titulo, String subtitulo) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cTitulo = new PdfPCell(new Phrase("LOTESMART ENTERPRISE - CONTROL DE CADUCIDAD FEFO", FONT_SUBTITULO));
        cTitulo.setBorder(0);
        cTitulo.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cTitulo);

        PdfPCell cSub = new PdfPCell(new Phrase(titulo, FONT_TITULO));
        cSub.setBorder(0);
        cSub.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSub.setPaddingTop(4);
        table.addCell(cSub);

        String infoGen = subtitulo + " | Generado por: " + SesionActual.getInstance().getUsername() +
                " | " + LocalDateTime.now().format(FECHA_HORA_FMT);
        PdfPCell cGen = new PdfPCell(new Phrase(infoGen, FONT_TEXTO));
        cGen.setBorder(0);
        cGen.setHorizontalAlignment(Element.ALIGN_CENTER);
        cGen.setPaddingTop(4);
        cGen.setPaddingBottom(8);
        table.addCell(cGen);

        return table;
    }

    private PdfPCell crearCeldaInfo(String contenido) {
        PdfPCell cell = new PdfPCell(new Phrase(contenido, FONT_TEXTO));
        cell.setBackgroundColor(new Color(245, 247, 250));
        cell.setPadding(8);
        cell.setBorderColor(new Color(210, 220, 230));
        return cell;
    }

    private PdfPCell crearCeldaTexto(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_TEXTO));
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(5);
        cell.setBorderColor(new Color(230, 235, 240));
        return cell;
    }
}
