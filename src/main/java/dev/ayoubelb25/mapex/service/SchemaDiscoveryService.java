package dev.ayoubelb25.mapex.service;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.ayoubelb25.mapex.model.mapping.ColumnMeta;
import dev.ayoubelb25.mapex.model.mapping.EntityInfo;
import dev.ayoubelb25.mapex.util.DBConfig;

/**
 * Discovers entities and mappable columns from the JPA metamodel.
 */
@Component
public class SchemaDiscoveryService {

    private static final Set<Attribute.PersistentAttributeType> RELATIONSHIP_TYPES = Set.of(
            Attribute.PersistentAttributeType.ONE_TO_MANY,
            Attribute.PersistentAttributeType.MANY_TO_ONE,
            Attribute.PersistentAttributeType.ONE_TO_ONE,
            Attribute.PersistentAttributeType.MANY_TO_MANY,
            Attribute.PersistentAttributeType.ELEMENT_COLLECTION);

    @Autowired
    private DBConfig dbConfig;

    @Autowired
    private ColumnDescriptionDAO columnDescriptionDAO;

    public List<EntityInfo> listEntities() {
        List<EntityInfo> entities = new ArrayList<>();
        EntityManager em = dbConfig.createEntityManager();
        try {
            Metamodel metamodel = em.getMetamodel();
            for (EntityType<?> entityType : metamodel.getEntities()) {
                Class<?> javaType = entityType.getJavaType();
                entities.add(new EntityInfo(
                        entityType.getName(),
                        resolveTableName(javaType),
                        entityType.getName()));
            }
        } finally {
            em.close();
        }
        return entities;
    }

    public List<ColumnMeta> getMappableColumns(String entityName) {
        List<ColumnMeta> columns = new ArrayList<>();
        EntityManager em = dbConfig.createEntityManager();
        try {
            EntityType<?> entityType = findEntityType(em.getMetamodel(), entityName);
            if (entityType == null) {
                return columns;
            }

            String tableName = resolveTableName(entityType.getJavaType());
            Map<String, String> comments = columnDescriptionDAO.getColumnDescriptions(tableName);

            for (Attribute<?, ?> attribute : entityType.getDeclaredAttributes()) {
                if (RELATIONSHIP_TYPES.contains(attribute.getPersistentAttributeType())) {
                    continue;
                }

                Field field = asField(attribute.getJavaMember());
                if (field == null || field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                Column columnAnn = field.getAnnotation(Column.class);
                String columnName = (columnAnn != null && !columnAnn.name().isEmpty())
                        ? columnAnn.name()
                        : attribute.getName();
                boolean nullable = columnAnn == null || columnAnn.nullable();

                columns.add(new ColumnMeta(
                        columnName,
                        attribute.getJavaType().getSimpleName(),
                        nullable,
                        comments.get(columnName)));
            }
        } finally {
            em.close();
        }
        return columns;
    }

    private EntityType<?> findEntityType(Metamodel metamodel, String entityName) {
        for (EntityType<?> entityType : metamodel.getEntities()) {
            if (entityType.getName().equalsIgnoreCase(entityName)) {
                return entityType;
            }
        }
        return null;
    }

    private String resolveTableName(Class<?> entityClass) {
        Table tableAnn = entityClass.getAnnotation(Table.class);
        return (tableAnn != null && !tableAnn.name().isEmpty())
                ? tableAnn.name()
                : entityClass.getSimpleName().toLowerCase();
    }

    private Field asField(Member member) {
        return (member instanceof Field) ? (Field) member : null;
    }
}
