package com.loanshark.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class DocumentParsingService {

    public IdentityDocumentAnalysis analyze(Path pdfPath, String firstName, String lastName, String idNumber) {
        String extractedText = extractPdfText(pdfPath);
        String normalizedText = normalize(extractedText);

        boolean firstNameMatch = normalizedText.contains(normalize(firstName));
        boolean lastNameMatch = normalizedText.contains(normalize(lastName));
        boolean idNumberMatch = normalizedText.contains(normalize(idNumber));
        boolean detailsMatched = firstNameMatch && lastNameMatch && idNumberMatch;

        return new IdentityDocumentAnalysis(
            extractedText,
            extractedText.isBlank() ? 0.00 : 70.00,
            firstNameMatch ? firstName : null,
            lastNameMatch ? lastName : null,
            idNumberMatch ? idNumber : null,
            detailsMatched
        );
    }

    private String extractPdfText(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException exception) {
            return "";
        }
    }

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    public record IdentityDocumentAnalysis(
        String extractedText,
        double confidence,
        String extractedFirstName,
        String extractedLastName,
        String extractedIdNumber,
        boolean detailsMatched
    ) {
    }
}
