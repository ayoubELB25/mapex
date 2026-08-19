package dev.ayoubelb25.mapex.model.mapping;

import java.util.List;

public class EntityMappingContext {

    private final String entityName;
    private final String tableName;
    private final String displayLabel;
    private final List<ColumnMeta> columns;

    public EntityMappingContext(String entityName, String tableName, String displayLabel,
            List<ColumnMeta> columns) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.displayLabel = displayLabel;
        this.columns = columns;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public List<ColumnMeta> getColumns() {
        return columns;
    }
}
