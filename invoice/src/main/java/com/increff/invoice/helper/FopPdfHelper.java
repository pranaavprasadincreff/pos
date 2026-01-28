package com.increff.invoice.helper;

import com.increff.pos.model.exception.ApiException;
import org.apache.fop.apps.*;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FopPdfHelper {
    private static final String INVOICE_DIR = "invoices/";

    static {
        try {
            Files.createDirectories(Paths.get(INVOICE_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create invoice directory", e);
        }
    }

    public static String generatePdfBase64(String invoiceFileName, String xslFoContent) throws ApiException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, outStream);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transform

            Source src = new StreamSource(new StringReader(xslFoContent));
            Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            byte[] pdfBytes = outStream.toByteArray();
            String pdfPath = INVOICE_DIR + invoiceFileName + ".pdf";
            Files.write(Paths.get(pdfPath), pdfBytes);

            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (Exception e) {
            throw new ApiException("Invoice PDF generation failed: " + e.getMessage());
        }
    }
}
