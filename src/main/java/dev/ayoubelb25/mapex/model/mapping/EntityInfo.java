package dev.ayoubelb25.mapex.model.mapping;

public class EntityInfo {

    private final String name;
    private final String tableName;
    private final String displayLabel;

    public EntityInfo(String name, String tableName, String displayLabel) {
        this.name = name;
        this.tableName = tableName;
        this.displayLabel = displayLabel;
    }

    public String getName() {
        return name;
    }

    public String getTableName() {
        return tableName;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
