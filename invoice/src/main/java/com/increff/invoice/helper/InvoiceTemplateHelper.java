package com.increff.invoice.helper;

import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.model.form.InvoiceItemForm;

import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class InvoiceTemplateHelper {

    private static final DecimalFormat moneyFormat = new DecimalFormat("#0.00");

    public static String createInvoiceXslFo(InvoiceGenerateForm form) {

        StringBuilder itemsTable = new StringBuilder();
        int serialNumber = 1;
        double invoiceTotalAmount = 0.0;

        for (InvoiceItemForm item : form.getItems()) {
            double lineTotal = item.getQuantity() * item.getSellingPrice();
            invoiceTotalAmount += lineTotal;

            itemsTable.append("<fo:table-row>")
                    .append(cell(String.valueOf(serialNumber++), "center"))
                    .append(cell(escape(item.getBarcode()), "left"))
                    .append(cell(escape(item.getProductName()), "left"))
                    .append(cell(String.valueOf(item.getQuantity()), "right"))
                    .append(cell(moneyFormat.format(item.getSellingPrice()), "right"))
                    .append(cell(moneyFormat.format(lineTotal), "right"))
                    .append("</fo:table-row>");
        }

        ZonedDateTime now = ZonedDateTime.now();
        String orderDate = formatInvoiceDate(now);

        String orderReferenceNumber = safe(form.getOrderReferenceId());

        // -------- XSL-FO (UNCHANGED LAYOUT) ----------
        String xslFo =
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<fo:root xmlns:fo='http://www.w3.org/1999/XSL/Format'>\n" +
                        "  <fo:layout-master-set>\n" +
                        "    <fo:simple-page-master master-name='A4' page-height='29.7cm' page-width='21cm' margin='1.5cm'>\n" +
                        "      <fo:region-body/>\n" +
                        "    </fo:simple-page-master>\n" +
                        "  </fo:layout-master-set>\n" +
                        "  <fo:page-sequence master-reference='A4'>\n" +
                        "    <fo:flow flow-name='xsl-region-body' font-family='Helvetica'>\n" +

                        // Top: Title + meta (right)
                        // Give more space to the right meta area so DATE always fits in a single line
                        "      <fo:table table-layout='fixed' width='100%'>\n" +
                        "        <fo:table-column column-width='55%'/>\n" +
                        "        <fo:table-column column-width='45%'/>\n" +
                        "        <fo:table-body>\n" +
                        "          <fo:table-row>\n" +
                        "            <fo:table-cell>\n" +
                        "              <fo:block font-size='20pt' font-weight='bold' color='#9E9E9E' margin-bottom='6pt'>Invoice</fo:block>\n" +
                        "            </fo:table-cell>\n" +
                        "            <fo:table-cell>\n" +
                        "              <fo:block margin-top='2pt'>\n" +
                        // Slightly wider value column + extra padding for breathing space
                        "                <fo:table table-layout='fixed' width='100%' border='0.5pt solid #BDBDBD'>\n" +
                        "                  <fo:table-column column-width='50%'/>\n" +
                        "                  <fo:table-column column-width='50%'/>\n" +
                        "                  <fo:table-body>\n" +
                        metaRow("DATE", orderDate) +
                        metaRow("ORDER REFERENCE NO.", orderReferenceNumber) +
                        "                  </fo:table-body>\n" +
                        "                </fo:table>\n" +
                        "              </fo:block>\n" +
                        "            </fo:table-cell>\n" +
                        "          </fo:table-row>\n" +
                        "        </fo:table-body>\n" +
                        "      </fo:table>\n" +

                        // Items table (grid)
                        "      <fo:block margin-top='14pt'>\n" +
                        "        <fo:table table-layout='fixed' width='100%' border='0.5pt solid #BDBDBD' border-collapse='separate'>\n" +
                        "          <fo:table-column column-width='6%'/>\n" +
                        "          <fo:table-column column-width='20%'/>\n" +
                        "          <fo:table-column column-width='30%'/>\n" +
                        "          <fo:table-column column-width='10%'/>\n" +
                        "          <fo:table-column column-width='17%'/>\n" +
                        "          <fo:table-column column-width='17%'/>\n" +
                        "          <fo:table-header>\n" +
                        "            <fo:table-row background-color='#F2F2F2' font-weight='bold'>\n" +
                        headerCell("S.No", "center") +
                        headerCell("Product Barcode", "left") +
                        headerCell("Product Name", "left") +
                        headerCell("Quantity", "right") +
                        headerCell("Price", "right") +
                        headerCell("Total", "right") +
                        "            </fo:table-row>\n" +
                        "          </fo:table-header>\n" +
                        "          <fo:table-body>\n" +
                        itemsTable +
                        "          </fo:table-body>\n" +
                        "        </fo:table>\n" +
                        "      </fo:block>\n" +

                        // Bottom area: remarks (left) + total (right)
                        "      <fo:block margin-top='12pt'>\n" +
                        "        <fo:table table-layout='fixed' width='100%'>\n" +
                        "          <fo:table-column column-width='60%'/>\n" +
                        "          <fo:table-column column-width='40%'/>\n" +
                        "          <fo:table-body>\n" +
                        "            <fo:table-row>\n" +
                        "              <fo:table-cell padding-right='10pt'>\n" +
                        "                <fo:block font-size='9pt' color='#666666'>Remarks / Instructions:</fo:block>\n" +
                        "                <fo:block font-size='9pt' color='#444444' margin-top='4pt'>Thank you for your purchase.</fo:block>\n" +
                        "                <fo:block font-size='9pt' color='#444444'>Please retain this invoice for your records.</fo:block>\n" +
                        "              </fo:table-cell>\n" +
                        "              <fo:table-cell>\n" +
                        "                <fo:table table-layout='fixed' width='100%'>\n" +
                        "                  <fo:table-column column-width='60%'/>\n" +
                        "                  <fo:table-column column-width='40%'/>\n" +
                        "                  <fo:table-body>\n" +
                        totalsRowOnly("TOTAL", moneyFormat.format(invoiceTotalAmount)) +
                        "                  </fo:table-body>\n" +
                        "                </fo:table>\n" +
                        "              </fo:table-cell>\n" +
                        "            </fo:table-row>\n" +
                        "          </fo:table-body>\n" +
                        "        </fo:table>\n" +
                        "      </fo:block>\n" +

                        "    </fo:flow>\n" +
                        "  </fo:page-sequence>\n" +
                        "</fo:root>";

        return xslFo;
    }

    private static String formatInvoiceDate(ZonedDateTime now) {
        int day = now.getDayOfMonth();
        String daySuffix = calculateDaySuffix(day);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");

        return timeFormatter.format(now) + ", " + day + daySuffix + " " + dateFormatter.format(now);
    }

    private static String calculateDaySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    // ---------- Helpers (unchanged markup) ----------
    private static String headerCell(String text, String align) {
        return "<fo:table-cell border='0.5pt solid #BDBDBD' padding='6pt'>" +
                "<fo:block font-size='9pt' text-align='" + align + "'>" + escape(text) + "</fo:block>" +
                "</fo:table-cell>";
    }

    private static String cell(String text, String align) {
        return "<fo:table-cell border='0.5pt solid #BDBDBD' padding='6pt'>" +
                "<fo:block font-size='9pt' text-align='" + align + "'>" + escape(text) + "</fo:block>" +
                "</fo:table-cell>";
    }

    private static String metaRow(String label, String value) {
        // wrap-option="no-wrap" ensures single-line; slightly smaller font to handle worst-case strings.
        return "                    <fo:table-row>\n" +
                "                      <fo:table-cell border='0.5pt solid #BDBDBD' padding-top='6pt' padding-bottom='6pt' padding-left='8pt' padding-right='8pt'>\n" +
                "                        <fo:block font-size='8pt' font-weight='bold' color='#666666' wrap-option='no-wrap'>" + escape(label) + "</fo:block>\n" +
                "                      </fo:table-cell>\n" +
                "                      <fo:table-cell border='0.5pt solid #BDBDBD' padding-top='6pt' padding-bottom='6pt' padding-left='8pt' padding-right='8pt'>\n" +
                "                        <fo:block font-size='8.5pt' text-align='right' wrap-option='no-wrap'>" + escape(value) + "</fo:block>\n" +
                "                      </fo:table-cell>\n" +
                "                    </fo:table-row>\n";
    }

    private static String totalsRowOnly(String label, String value) {
        return "                    <fo:table-row>\n" +
                "                      <fo:table-cell padding='6pt'>\n" +
                "                        <fo:block font-size='8pt' color='#666666' font-weight='bold' text-align='right' wrap-option='no-wrap'>" + escape(label) + "</fo:block>\n" +
                "                      </fo:table-cell>\n" +
                "                      <fo:table-cell padding='6pt' background-color='#F2F2F2'>\n" +
                "                        <fo:block font-size='10pt' text-align='right' font-weight='bold' wrap-option='no-wrap'>" + escape(value) + "</fo:block>\n" +
                "                      </fo:table-cell>\n" +
                "                    </fo:table-row>\n";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // Minimal XML escaping for safety
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
