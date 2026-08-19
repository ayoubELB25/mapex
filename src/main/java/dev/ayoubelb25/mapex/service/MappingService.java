package dev.ayoubelb25.mapex.service;

import dev.ayoubelb25.mapex.model.MappingResult;
import dev.ayoubelb25.mapex.model.mapping.ColumnMeta;
import dev.ayoubelb25.mapex.service.PdfExtractorService;

//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.inject.Inject;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.persistence.Id;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

@Component
public class MappingService {

    @Autowired
    private SchemaMapperService schemaMapperService;

    @Autowired
    private OllamaClient ollamaClient;

    @Autowired
    private PdfExtractorService pdfExtractorService;

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private PromptBuilder promptBuilder;

    public List<String> getEntityColumns(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> !field.isAnnotationPresent(Transient.class))
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .map(field -> {
                    Column col = field.getAnnotation(Column.class);
                    return (col != null && !col.name().isEmpty())
                            ? col.name()
                            : field.getName();
                })
                .filter(name -> !name.equals("status"))
                .collect(Collectors.toList());
    }

    public MappingResult suggestCsvMapping(List<String> csvHeaders, List<String> dbColumns,
            String provider, String model, String apiKey, String apiUrl) {
        MappingResult result = new MappingResult();
        try {
            String prompt = schemaMapperService.buildMappingPrompt(csvHeaders, dbColumns);
            String rawResponse = callModel(provider, model, prompt, apiKey, apiUrl);
            result.setRawResponse(rawResponse);
            result.setMappings(schemaMapperService.parseOllamaResponse(rawResponse));
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    public MappingResult suggestPdfMapping(File pdfFile, List<String> dbColumns,
            String provider, String model, String apiKey, String apiUrl, String targetEntity) {
        try {
            return suggestPdfMapping(Files.readAllBytes(pdfFile.toPath()), dbColumns,
                    provider, model, apiKey, apiUrl, targetEntity);
        } catch (IOException e) {
            MappingResult result = new MappingResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    public MappingResult suggestPdfMapping(byte[] pdfBytes, List<String> dbColumns,
            String provider, String model, String apiKey, String apiUrl, String targetEntity) {
        MappingResult result = new MappingResult();
        try {
            String fullText = pdfExtractorService.extract(pdfBytes);
            String prompt = schemaMapperService.buildPdfPrompt(fullText, dbColumns, targetEntity);
            String rawResponse = callModel(provider, model, prompt, apiKey, apiUrl);
            result.setRawResponse(rawResponse);
            schemaMapperService.parsePdfIntoResult(rawResponse, result);
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    /**
     * Builds a PDF mapping result from extracted text and target column metadata.
     *
     * @param userInstructions optional free-text context from the analyst;
     *                         may be null or blank
     */
    public MappingResult mapPdf(byte[] pdfBytes, List<ColumnMeta> targetColumns,
            String provider, String model, String apiKey, String apiUrl, String userInstructions) {
        MappingResult result = new MappingResult();
        try {
            String fullText = pdfExtractorService.extract(pdfBytes);
            String prompt = promptBuilder.buildPdfPrompt(targetColumns, fullText, userInstructions);
            String rawResponse = callModel(provider, model, prompt, apiKey, apiUrl);
            result.setRawResponse(rawResponse);
            schemaMapperService.parsePdfIntoResult(rawResponse, result);
            discardUnusableNormalizations(result, targetColumns);
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    /**
     * Removes normalization proposals that are incomplete or reference unknown columns.
     */
    private void discardUnusableNormalizations(MappingResult result, List<ColumnMeta> targetColumns) {
        if (result.getValueNormalizations() == null || result.getValueNormalizations().isEmpty()) {
            return;
        }
        Set<String> known = targetColumns.stream()
                .map(c -> c.getColumnName().toLowerCase())
                .collect(Collectors.toSet());

        result.getValueNormalizations().removeIf(n
                -> n.getInternalField() == null || n.getInternalField().isBlank()
                || n.getSourceValue() == null || n.getSourceValue().isBlank()
                || n.getNormalizedValue() == null || n.getNormalizedValue().isBlank()
                || !known.contains(n.getInternalField().toLowerCase()));
    }

    private String callModel(String provider, String model, String prompt,
            String apiKey, String apiUrl) throws Exception {
        if ("gemini".equals(provider)) {
            return geminiClient.generate(model, prompt, apiKey, apiUrl);
        }
        return ollamaClient.generate(model, prompt);
    }
}
