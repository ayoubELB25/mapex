package dev.ayoubelb25.mapex.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MappingResult {

    private List<FieldMapping> mappings = new ArrayList<>();
    private String rawResponse;
    private boolean success;
    private String errorMessage;
    private List<String> missingFields = new ArrayList<>();
    private List<Map<String, String>> extractedRows = new ArrayList<>();
    private String freeText;
    private List<ValueNormalization> valueNormalizations = new ArrayList<>();
    private List<Clarification> clarificationsNeeded = new ArrayList<>();

    /**
     * Proposed normalization for a raw source value.
     */
    public static class ValueNormalization {

        private String internalField;
        private String sourceValue;
        private String normalizedValue;
        private double confidence;
        private boolean accepted = true; // Can be rejected before insert.

        public ValueNormalization() {
        }

        public ValueNormalization(String internalField, String sourceValue,
                String normalizedValue, double confidence) {
            this.internalField = internalField;
            this.sourceValue = sourceValue;
            this.normalizedValue = normalizedValue;
            this.confidence = confidence;
        }

        public String getConfidenceLevel() {
            if (confidence >= 0.8) {
                return "success";
            }
            if (confidence >= 0.5) {
                return "warn";
            }
            return "danger";
        }

        public String getInternalField() {
            return internalField;
        }

        public void setInternalField(String v) {
            this.internalField = v;
        }

        public String getSourceValue() {
            return sourceValue;
        }

        public void setSourceValue(String v) {
            this.sourceValue = v;
        }

        public String getNormalizedValue() {
            return normalizedValue;
        }

        public void setNormalizedValue(String v) {
            this.normalizedValue = v;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double v) {
            this.confidence = v;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public void setAccepted(boolean v) {
            this.accepted = v;
        }
    }

    /**
     * A clarification request for values the model could not resolve.
     */
    public static class Clarification {

        private String internalField;
        private List<String> sourceValues = new ArrayList<>();
        private String question;

        public Clarification() {
        }

        public Clarification(String internalField, List<String> sourceValues, String question) {
            this.internalField = internalField;
            this.sourceValues = sourceValues;
            this.question = question;
        }

        public String getInternalField() {
            return internalField;
        }

        public void setInternalField(String v) {
            this.internalField = v;
        }

        public List<String> getSourceValues() {
            return sourceValues;
        }

        public void setSourceValues(List<String> v) {
            this.sourceValues = v;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String v) {
            this.question = v;
        }
    }

    public static class FieldMapping {

        private String clientField;
        private String internalField;
        private double confidence;
        private String overriddenField; // User override, when provided.

        public FieldMapping() {
        }

        public FieldMapping(String clientField, String internalField, double confidence) {
            this.clientField = clientField;
            this.internalField = internalField;
            this.confidence = confidence;

        }

        /**
         * Returns the override field when present, otherwise the suggested field.
         */
        public String getEffectiveField() {
            return (overriddenField != null && !overriddenField.isBlank())
                    ? overriddenField : internalField;
        }

        public String getConfidenceLevel() {
            if (confidence >= 0.8) {
                return "success";
            }
            if (confidence >= 0.5) {
                return "warn";
            }
            return "danger";
        }

        public String getClientField() {
            return clientField;
        }

        public String getInternalField() {
            return internalField;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getOverriddenField() {
            return overriddenField;
        }

        public void setClientField(String v) {
            this.clientField = v;
        }

        public void setInternalField(String v) {
            this.internalField = v;
        }

        public void setConfidence(double v) {
            this.confidence = v;
        }

        public void setOverriddenField(String v) {
            this.overriddenField = v;
        }

        public String getSourceColumn() {
            return clientField;
        }

        public String getTargetColumn() {
            return internalField;
        }
    }

    public List<FieldMapping> getMappings() {
        return mappings;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setMappings(List<FieldMapping> v) {
        this.mappings = v;
    }

    public void setRawResponse(String v) {
        this.rawResponse = v;
    }

    public void setSuccess(boolean v) {
        this.success = v;
    }

    public void setErrorMessage(String v) {
        this.errorMessage = v;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> v) {
        this.missingFields = v;
    }

    public boolean hasMissingFields() {
        return missingFields != null && !missingFields.isEmpty();
    }

    public List<Map<String, String>> getExtractedRows() {
        return extractedRows;
    }

    public void setExtractedRows(List<Map<String, String>> v) {
        this.extractedRows = v;
    }

    public String getFreeText() {
        return freeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public List<ValueNormalization> getValueNormalizations() {
        return valueNormalizations;
    }

    public void setValueNormalizations(List<ValueNormalization> v) {
        this.valueNormalizations = v;
    }

    public List<Clarification> getClarificationsNeeded() {
        return clarificationsNeeded;
    }

    public void setClarificationsNeeded(List<Clarification> v) {
        this.clarificationsNeeded = v;
    }

}
