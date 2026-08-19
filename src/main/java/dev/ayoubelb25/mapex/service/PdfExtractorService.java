package dev.ayoubelb25.mapex.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfExtractorService {

    public static class PdfExtractionResult {

        private final Map<String, String> structuredFields;
        private final String freeText;

        public PdfExtractionResult(Map<String, String> structuredFields, String freeText) {
            this.structuredFields = structuredFields;
            this.freeText = freeText;
        }

        public Map<String, String> getStructuredFields() {
            return structuredFields;
        }

        public String getFreeText() {
            return freeText;
        }
    }

    public String extract(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            if (doc.isEncrypted()) {
                throw new IllegalStateException("PDF is encrypted — cannot extract text.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private PdfExtractionResult splitStructuredFromFreeText(String text) {
        Map<String, String> structured = new LinkedHashMap<>();
        StringBuilder freeText = new StringBuilder();

        if (text == null || text.isBlank()) {
            return new PdfExtractionResult(structured, "");
        }

        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            // matches "Label: Value" or "Label : Value", label max 50 chars
            if (trimmed.matches("^[^:]{1,50}:\\s*.+$")) {
                String[] parts = trimmed.split(":\\s*", 2);
                structured.put(parts[0].trim(), parts[1].trim());
            } else if (!trimmed.isEmpty()) {
                freeText.append(trimmed).append("\n");
            }
        }

        return new PdfExtractionResult(structured, freeText.toString().trim());
    }
}
