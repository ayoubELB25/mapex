package dev.ayoubelb25.mapex.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import dev.ayoubelb25.mapex.model.mapping.ColumnMeta;

/**
 * Builds mapping prompts from classpath templates and target column metadata.
 */
@Component
public class PromptBuilder {

    private static final String MAPPING_TEMPLATE = "prompts/mapping-prompt-template.txt";
    private static final String PDF_TEMPLATE = "prompts/pdf-prompt-template.txt";

    /** Maximum number of characters retained from extracted document text. */
    private static final int MAX_DOCUMENT_CHARS = 3000;

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    /**
     * Builds a mapping-only prompt for CSV or JSON sources.
     */
    public String buildMappingPrompt(List<ColumnMeta> targetColumns, List<String> sourceHeaders,
            String userInstructions) {
        return loadTemplate(MAPPING_TEMPLATE)
                .replace("{{schema}}", renderSchema(targetColumns))
                .replace("{{sourceColumns}}", String.valueOf(sourceHeaders))
                .replace("{{userInstructions}}", renderUserInstructions(userInstructions));
    }

    /**
     * Builds a PDF prompt that maps fields and extracts records.
     */
    public String buildPdfPrompt(List<ColumnMeta> targetColumns, String documentText,
            String userInstructions) {
        return loadTemplate(PDF_TEMPLATE)
                .replace("{{schema}}", renderSchema(targetColumns))
                .replace("{{documentText}}", truncate(documentText, MAX_DOCUMENT_CHARS))
                .replace("{{userInstructions}}", renderUserInstructions(userInstructions));
    }

    /**
     * Renders target columns as a bullet list for the prompt.
     */
    private String renderSchema(List<ColumnMeta> targetColumns) {
        StringBuilder schema = new StringBuilder();
        for (ColumnMeta col : targetColumns) {
            schema.append("- ").append(col.getColumnName());
            if (col.getJavaType() != null && !col.getJavaType().isBlank()) {
                schema.append(" [").append(col.getJavaType());
                if (!col.isNullable()) {
                    schema.append(", required");
                }
                schema.append("]");
            }
            if (col.getDbComment() != null && !col.getDbComment().isBlank()) {
                schema.append(" : ").append(col.getDbComment().trim());
            }
            schema.append("\n");
        }
        return schema.toString().trim();
    }

    /**
     * Normalizes the user instruction block when no text is provided.
     */
    private String renderUserInstructions(String userInstructions) {
        return (userInstructions == null || userInstructions.isBlank())
                ? "(none provided)"
                : userInstructions.trim();
    }

    private String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private String loadTemplate(String path) {
        return templateCache.computeIfAbsent(path, p -> {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(p)) {
                if (is == null) {
                    throw new IllegalStateException("Prompt template not found on classpath: " + p);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load prompt template: " + p, e);
            }
        });
    }
}
