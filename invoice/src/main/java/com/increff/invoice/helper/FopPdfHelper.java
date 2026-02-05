package com.increff.invoice.helper;

import com.increff.pos.model.exception.ApiException;
import org.apache.fop.apps.*;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class FopPdfHelper {

    private static final Path INVOICE_DIRECTORY = Paths.get("invoices");

    static {
        try {
            Files.createDirectories(INVOICE_DIRECTORY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create invoices directory", e);
        }
    }

    public static String buildPdfPath(String orderReferenceId) {
        return INVOICE_DIRECTORY.resolve(orderReferenceId + ".pdf").toString();
    }

    public static String generatePdfAndReturnBase64(String orderReferenceId, String xslFoContent) throws ApiException {
        try (ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

            FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
            FOUserAgent userAgent = fopFactory.newFOUserAgent();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, pdfOutputStream);

            Transformer transformer = TransformerFactory.newInstance().newTransformer(); // identity transform
            Source foSource = new StreamSource(new StringReader(xslFoContent));
            Result pdfResult = new SAXResult(fop.getDefaultHandler());

            transformer.transform(foSource, pdfResult);

            byte[] pdfBytes = pdfOutputStream.toByteArray();
            Files.write(Paths.get(buildPdfPath(orderReferenceId)), pdfBytes);

            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (Exception e) {
            throw new ApiException("Invoice PDF generation failed: " + e.getMessage());
        }
    }
}
