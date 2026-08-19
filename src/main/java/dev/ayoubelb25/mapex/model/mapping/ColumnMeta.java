package dev.ayoubelb25.mapex.model.mapping;

public class ColumnMeta {

    private final String columnName;
    private final String javaType;
    private final boolean nullable;
    private final String dbComment;

    public ColumnMeta(String columnName, String javaType, boolean nullable, String dbComment) {
        this.columnName = columnName;
        this.javaType = javaType;
        this.nullable = nullable;
        this.dbComment = dbComment;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getJavaType() {
        return javaType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getDbComment() {
        return dbComment;
    }
}
