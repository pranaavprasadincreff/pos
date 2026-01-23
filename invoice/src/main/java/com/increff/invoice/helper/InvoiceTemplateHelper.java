package com.increff.invoice.helper;

import com.increff.invoice.modal.form.InvoiceGenerateForm;
import com.increff.invoice.modal.form.InvoiceItemForm;

import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class InvoiceTemplateHelper {

    private static final DecimalFormat df = new DecimalFormat("#0.00");

    public static String createInvoiceXslFo(InvoiceGenerateForm form) {

        StringBuilder itemsTable = new StringBuilder();

        // Table header
        itemsTable.append("<fo:table-row font-weight='bold'>")
                .append("<fo:table-cell><fo:block>S.No</fo:block></fo:table-cell>")
                .append("<fo:table-cell><fo:block>Product Barcode</fo:block></fo:table-cell>")
                .append("<fo:table-cell><fo:block>Product Name</fo:block></fo:table-cell>")
                .append("<fo:table-cell><fo:block>Quantity</fo:block></fo:table-cell>")
                .append("<fo:table-cell><fo:block>Price</fo:block></fo:table-cell>")
                .append("<fo:table-cell><fo:block>Total</fo:block></fo:table-cell>")
                .append("</fo:table-row>");

        int sno = 1;
        double grandTotal = 0.0;

        for (InvoiceItemForm item : form.getItems()) {
            double total = item.getQuantity() * item.getSellingPrice();
            grandTotal += total;

            itemsTable.append("<fo:table-row>")
                    .append("<fo:table-cell><fo:block>").append(sno++).append("</fo:block></fo:table-cell>")
                    .append("<fo:table-cell><fo:block>").append(item.getBarcode()).append("</fo:block></fo:table-cell>")
                    .append("<fo:table-cell><fo:block>").append(item.getProductName()).append("</fo:block></fo:table-cell>")
                    .append("<fo:table-cell><fo:block>").append(item.getQuantity()).append("</fo:block></fo:table-cell>")
                    .append("<fo:table-cell><fo:block>").append(df.format(item.getSellingPrice())).append("</fo:block></fo:table-cell>")
                    .append("<fo:table-cell><fo:block>").append(df.format(total)).append("</fo:block></fo:table-cell>")
                    .append("</fo:table-row>");
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String invoiceDate = dtf.format(ZonedDateTime.now());

        // Full XSL-FO document
        String xslFo =
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<fo:root xmlns:fo='http://www.w3.org/1999/XSL/Format'>\n" +
                        "  <fo:layout-master-set>\n" +
                        "    <fo:simple-page-master master-name='A4' page-height='29.7cm' page-width='21cm' margin='2cm'>\n" +
                        "      <fo:region-body/>\n" +
                        "    </fo:simple-page-master>\n" +
                        "  </fo:layout-master-set>\n" +
                        "  <fo:page-sequence master-reference='A4'>\n" +
                        "    <fo:flow flow-name='xsl-region-body'>\n" +
                        "      <fo:block font-size='20pt' font-weight='bold' text-align='center' margin-bottom='10pt'>Invoice</fo:block>\n" +
                        "      <fo:block>Invoice Date: " + invoiceDate + "</fo:block>\n" +
                        "      <fo:block>Order Reference ID: " + form.getOrderReferenceId() + "</fo:block>\n" +
                        "      <fo:block margin-top='10pt'>\n" +
                        "        <fo:table table-layout='fixed' width='100%' border='0.5pt solid black' border-collapse='collapse'>\n" +
                        "          <fo:table-column column-width='5%'/>\n" +
                        "          <fo:table-column column-width='20%'/>\n" +
                        "          <fo:table-column column-width='25%'/>\n" +
                        "          <fo:table-column column-width='10%'/>\n" +
                        "          <fo:table-column column-width='20%'/>\n" +
                        "          <fo:table-column column-width='20%'/>" +
                        "          <fo:table-body>\n" +
                        itemsTable.toString() +
                        "            <fo:table-row font-weight='bold'>\n" +
                        "              <fo:table-cell number-columns-spanned='5'><fo:block text-align='right'>Grand Total</fo:block></fo:table-cell>\n" +
                        "              <fo:table-cell><fo:block>" + df.format(grandTotal) + "</fo:block></fo:table-cell>\n" +
                        "            </fo:table-row>\n" +
                        "          </fo:table-body>\n" +
                        "        </fo:table>\n" +
                        "      </fo:block>\n" +
                        "    </fo:flow>\n" +
                        "  </fo:page-sequence>\n" +
                        "</fo:root>";

        return xslFo;
    }
}