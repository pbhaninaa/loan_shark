package com.loanshark.api.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class PdfUtils {

    public static String extractTextFromBase64Pdf(String base64Pdf) throws Exception {
        if (base64Pdf.contains(",")) {
            base64Pdf = base64Pdf.split(",")[1]; // Remove data prefix
        }
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

        try (PDDocument document = Loader.loadPDF((RandomAccessRead) new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}