package dev.ayoubelb25.mapex.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.ayoubelb25.mapex.model.MappingResult;
import dev.ayoubelb25.mapex.model.MappingResult.FieldMapping;

@Component
public class SchemaMapperService {

    public static final String MODEL = "mistral:7b";
    private static final int MAX_ROWS = 6;   // context window budget
    private static final int MAX_CHARS = 1600;

    private static final String PDF_EXAMPLE_TRANSACTION
            = "{\"mappings\":[{\"clientField\":\"Code valeur\",\"internalField\":\"isin\",\"confidence\":0.98},"
            + "{\"clientField\":\"Sens opération\",\"internalField\":\"side\",\"confidence\":0.95}],"
            + "\"rows\":[{\"Code valeur\":\"MA0000011488\",\"Sens opération\":\"Achat\","
            + "\"Nombre titres\":\"500\",\"Cours d'exécution\":\"142.50\",\"Nature ordre\":\"Au marché\"}]}";

    private static final String PDF_EXAMPLE_CLIENT
            = "{\"mappings\":[{\"clientField\":\"Raison sociale\",\"internalField\":\"name\",\"confidence\":0.97},"
            + "{\"clientField\":\"N° RC\",\"internalField\":\"registration_number\",\"confidence\":0.9}],"
            + "\"rows\":[{\"Raison sociale\":\"Attijariwafa Bank\",\"N° RC\":\"12345\","
            + "\"Code\":\"AWB01\",\"Email\":\"contact@awb.ma\"}]}";

    @Autowired
    private OllamaClient ollamaClient;

    @Autowired
    private ColumnDescriptionDAO columnDescriptionDAO;

    /**
     * Builds a prompt for the requested data type.
     */

    public String buildMappingPrompt(List<String> sourceColumns, List<String> dbColumns) {
        StringBuilder schema = new StringBuilder();
        for (String col : dbColumns) {
            schema.append("- ").append(col).append("\n");
        }
        return "You are a strict data mapping assistant.\n"
                + "Map incoming source columns to internal database fields.\n\n"
                + "INTERNAL FIELDS (map to these only):\n"
                + schema.toString().trim() + "\n\n"
                + "SOURCE COLUMNS: " + sourceColumns + "\n\n"
                + "STRICT RULES:\n"
                + "- Output ONLY a JSON array. No explanation, no markdown, no extra text.\n"
                + "- Every object MUST have: clientField, internalField, confidence (0.0–1.0).\n"
                + "- Map ONLY source columns that correspond to financial transaction data.\n"
                + "- only produce mappings you can find evidence for.\n"
                + "- If no reasonable match exists, set internalField to \"unknown\".\n"
                + "- Do NOT invent fields. Only use the internal fields listed above.\n"
                + "- Confidence 1.0 = exact match. 0.5 = plausible. Below 0.4 = weak.\n\n"
                + "Example: [{\"clientField\":\"Code valeur\",\"internalField\":\"isin\",\"confidence\":0.98},"
                + "{\"clientField\":\"Total operations\",\"internalField\":\"unknown\",\"confidence\":0.0}]\n\n"
                + "JSON array:\n";
    }

    public List<MappingResult.FieldMapping> parseOllamaResponse(String rawResponse) {
        return parseResponse(rawResponse);
    }

    private String buildPrompt(String data, String dataType, String schema) {
        return switch (dataType) {
            case "CSV" ->
                buildPromptForCsv(data, schema);
            case "JSON" ->
                buildPromptForJson(data, schema);
            case "PDF" ->
                buildPromptForPdf(data, schema, PDF_EXAMPLE_TRANSACTION);
            default ->
                buildPromptForCsv(data, schema);
        };
    }

    private String buildPromptForCsv(String csvData, String schema) {
        String sample = capLines(csvData, MAX_ROWS + 1);
        return "You are a data mapping assistant for a financial system.\n"
                + "The incoming data is a CSV file. The first row is the header with column names.\n\n"
                + "INTERNAL SCHEMA (target fields):\n" + schema + "\n\n"
                + "CSV SAMPLE (header + first rows):\n" + sample + "\n\n"
                + "Map each CSV column name to the closest internal field.\n"
                + "Return ONLY a JSON array. No explanation. No markdown.\n"
                + "Each object MUST have: clientField, internalField, confidence (0.0 to 1.0).\n"
                + "Example: [{\"clientField\":\"sens_ordre\",\"internalField\":\"side\",\"confidence\":0.91}]\n\n"
                + "JSON array:";
    }

    private String buildPromptForJson(String jsonData, String schema) {
        String sample = truncate(jsonData, MAX_CHARS);
        return "You are a data mapping assistant for a financial system.\n"
                + "The incoming data is a JSON array. Analyze the field names in the objects.\n\n"
                + "INTERNAL SCHEMA (target fields):\n" + schema + "\n\n"
                + "JSON SAMPLE (first objects):\n" + sample + "\n\n"
                + "Map each JSON field name to the closest internal field.\n"
                + "Return ONLY a JSON array. No explanation. No markdown.\n"
                + "Each object MUST have: clientField, internalField, confidence (0.0 to 1.0).\n"
                + "Example: [{\"clientField\":\"prix\",\"internalField\":\"price\",\"confidence\":0.95}]\n\n"
                + "JSON array:";
    }

    private String buildPromptForPdf(String pdfText, String schema, String exampleBlock) {
        String sample = truncate(pdfText, 3000);
        return "You are a data extraction assistant.\n"
                + "Extract records from the text below.\n\n"
                + "INTERNAL FIELDS — map to these ONLY:\n"
                + schema + "\n\n"
                + "RULES:\n"
                + "- Return ONLY valid JSON. No markdown, no explanation.\n"
                + "- mappings: one entry per internal field found. Include confidence (0.0-1.0).\n"
                + "- rows: extract every record. Use EXACT source label as key, raw value as value.\n"
                + "- In rows, every object must have the SAME keys — one per mapped field.\n"
                + "- Do NOT use colons inside JSON values. Keep raw values exactly as in the source.\n\n"
                + "Required JSON structure:\n" + exampleBlock + "\n\n"
                + "TEXT:\n" + sample + "\n\n"
                + "JSON object:";
    }

    /**
     * Builds the PDF prompt from the supplied database columns.
     */
    public String buildPdfPrompt(String pdfText, List<String> dbColumns, String targetEntity) {
        StringBuilder schema = new StringBuilder();
        for (String col : dbColumns) {
            schema.append("- ").append(col).append("\n");
        }
        String example = "CLIENT".equals(targetEntity) ? PDF_EXAMPLE_CLIENT : PDF_EXAMPLE_TRANSACTION;
        return buildPromptForPdf(pdfText, schema.toString().trim(), example);
    }

    /**
     * Parses a PDF response into an existing {@link MappingResult}.
     */
    public void parsePdfIntoResult(String rawResponse, MappingResult result) {
        parsePdfResponse(rawResponse, result);
    }

    /**
     * Renders database columns as a prompt-friendly list.
     */
    private String buildSchemaString(Map<String, String> schema) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            sb.append("- ").append(entry.getKey())
                    .append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString().trim();
    }

    private List<FieldMapping> parseResponse(String json) {
        List<FieldMapping> mappings = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return mappings;
        }

        String arrayJson = json.trim();
        if (!arrayJson.startsWith("[")) {
            int start = arrayJson.indexOf('[');
            int end = arrayJson.lastIndexOf(']');
            if (start == -1 || end == -1) {
                return mappings;
            }
            arrayJson = arrayJson.substring(start, end + 1);
        }

        String[] objects = arrayJson.split("\\},\\s*\\{");
        for (String obj : objects) {
            try {
                FieldMapping fm = parseObject(obj);
                if (fm != null) {
                    mappings.add(fm);
                }
            } catch (Exception ignored) {
            }
        }
        return mappings;
    }

    private void parsePdfResponse(String json, MappingResult result) {
        if (json == null || json.isBlank()) {
            return;
        }

        String cleaned = json.trim();
        int mStart = cleaned.indexOf("\"mappings\"");
        int rStart = cleaned.indexOf("\"rows\"");

        if (mStart != -1) {
            int arrStart = cleaned.indexOf('[', mStart);
            int arrEnd = findMatchingBracket(cleaned, arrStart);
            if (arrStart != -1 && arrEnd != -1) {
                result.setMappings(parseResponse(cleaned.substring(arrStart, arrEnd + 1)));
            }
        }

        if (rStart != -1) {
            int arrStart = cleaned.indexOf('[', rStart);
            int arrEnd = findMatchingBracket(cleaned, arrStart);
            if (arrStart != -1 && arrEnd != -1) {
                result.setExtractedRows(parseRowsArray(cleaned.substring(arrStart, arrEnd + 1)));
            }
        }

        int nStart = cleaned.indexOf("\"valueNormalizations\"");
        if (nStart != -1) {
            int arrStart = cleaned.indexOf('[', nStart);
            int arrEnd = findMatchingBracket(cleaned, arrStart);
            if (arrStart != -1 && arrEnd != -1) {
                result.setValueNormalizations(
                        parseNormalizations(cleaned.substring(arrStart, arrEnd + 1)));
            }
        }

        int cStart = cleaned.indexOf("\"clarificationsNeeded\"");
        if (cStart != -1) {
            int arrStart = cleaned.indexOf('[', cStart);
            int arrEnd = findMatchingBracket(cleaned, arrStart);
            if (arrStart != -1 && arrEnd != -1) {
                result.setClarificationsNeeded(
                        parseClarifications(cleaned.substring(arrStart, arrEnd + 1)));
            }
        }

        if (result.getMappings().isEmpty()
                && result.getExtractedRows() != null
                && !result.getExtractedRows().isEmpty()) {
            List<FieldMapping> auto = new ArrayList<>();
            for (String key : result.getExtractedRows().get(0).keySet()) {
                auto.add(new FieldMapping(key, key, 1.0));
            }
            result.setMappings(auto);
        }
    }

    private int findMatchingBracket(String s, int open) {
        if (open == -1) {
            return -1;
        }
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '[') {
                depth++;
            } else if (s.charAt(i) == ']') {
                if (--depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private List<Map<String, String>> parseRowsArray(String json) {
        List<Map<String, String>> rows = new ArrayList<>();
        System.out.println(">>> parseRowsArray input: " + json);
        try (jakarta.json.JsonReader reader = jakarta.json.Json
                .createReader(new java.io.ByteArrayInputStream(
                        json.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
            jakarta.json.JsonArray arr = reader.readArray();
            for (int i = 0; i < arr.size(); i++) {
                jakarta.json.JsonObject obj = arr.getJsonObject(i);
                Map<String, String> row = new java.util.LinkedHashMap<>();
                for (String key : obj.keySet()) {
                    jakarta.json.JsonValue val = obj.get(key);
                    row.put(key, val.getValueType() == jakarta.json.JsonValue.ValueType.STRING
                            ? obj.getString(key)
                            : val.toString().replace("\"", ""));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            System.err.println("[parseRowsArray] Failed: " + e.getMessage());
        }
        return rows;
    }

    /**
     * Parses the value-normalization dictionary and skips malformed entries.
     */
    private List<MappingResult.ValueNormalization> parseNormalizations(String json) {
        List<MappingResult.ValueNormalization> out = new ArrayList<>();
        try (jakarta.json.JsonReader reader = jakarta.json.Json
                .createReader(new java.io.ByteArrayInputStream(
                        json.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
            jakarta.json.JsonArray arr = reader.readArray();
            for (int i = 0; i < arr.size(); i++) {
                try {
                    jakarta.json.JsonObject obj = arr.getJsonObject(i);
                    out.add(new MappingResult.ValueNormalization(
                            obj.getString("internalField", null),
                            obj.getString("sourceValue", null),
                            obj.getString("normalizedValue", null),
                            readConfidence(obj)));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.err.println("[parseNormalizations] Failed: " + e.getMessage());
        }
        return out;
    }

    private List<MappingResult.Clarification> parseClarifications(String json) {
        List<MappingResult.Clarification> out = new ArrayList<>();
        try (jakarta.json.JsonReader reader = jakarta.json.Json
                .createReader(new java.io.ByteArrayInputStream(
                        json.getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
            jakarta.json.JsonArray arr = reader.readArray();
            for (int i = 0; i < arr.size(); i++) {
                try {
                    jakarta.json.JsonObject obj = arr.getJsonObject(i);
                    List<String> values = new ArrayList<>();
                    jakarta.json.JsonArray raw = obj.getJsonArray("sourceValues");
                    if (raw != null) {
                        for (int v = 0; v < raw.size(); v++) {
                            values.add(raw.getString(v));
                        }
                    }
                    out.add(new MappingResult.Clarification(
                            obj.getString("internalField", null),
                            values,
                            obj.getString("question", null)));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.err.println("[parseClarifications] Failed: " + e.getMessage());
        }
        return out;
    }

    /** Confidence may arrive as a JSON number or quoted string. */
    private double readConfidence(jakarta.json.JsonObject obj) {
        jakarta.json.JsonValue val = obj.get("confidence");
        if (val == null) {
            return 0.0;
        }
        try {
            if (val.getValueType() == jakarta.json.JsonValue.ValueType.NUMBER) {
                return ((jakarta.json.JsonNumber) val).doubleValue();
            }
            return Double.parseDouble(val.toString().replace("\"", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private FieldMapping parseObject(String obj) {
        String clientField = extractString(obj, "clientField");
        String internalField = extractString(obj, "internalField");
        double confidence = extractDouble(obj, "confidence");
        if (clientField == null || internalField == null) {
            return null;
        }
        return new FieldMapping(clientField, internalField, confidence);
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) {
            return null;
        }
        int valStart = json.indexOf('"', idx + search.length() + 1);
        if (valStart == -1) {
            return null;
        }
        int valEnd = json.indexOf('"', valStart + 1);
        if (valEnd == -1) {
            return null;
        }
        return json.substring(valStart + 1, valEnd).trim();
    }

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) {
            return 0.0;
        }
        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) {
            return 0.0;
        }
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private List<String> detectMissing(
            List<FieldMapping> mappings,
            java.util.Set<String> expectedFields) {

        java.util.Set<String> mapped = new java.util.HashSet<>();
        for (FieldMapping fm : mappings) {
            String effective = fm.getOverriddenField() != null
                    ? fm.getOverriddenField() : fm.getInternalField();
            if (effective != null && !effective.equals("unknown")) {
                mapped.add(effective);
            }
        }

        List<String> missing = new ArrayList<>();
        for (String expected : expectedFields) {
            if (!mapped.contains(expected)) {
                missing.add(expected);
            }
        }
        return missing;
    }

    /**
     * Keeps only the first N lines of the supplied text.
     */
    private String capLines(String text, int maxLines) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\n");
        int limit = Math.min(lines.length, maxLines);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().trim();
    }

    private String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private String buildPromptForPdf(String pdfText, String trim) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
