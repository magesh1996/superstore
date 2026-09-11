package com.superstore.app.config.hibernate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import jakarta.persistence.EmbeddedId;

@Component
public class CustomColumnOrder implements ColumnOrderingStrategy, HibernatePropertiesCustomizer {

    @Override
    public List<Column> orderTableColumns(Table table, Metadata metadata) {
        List<Column> columns = new ArrayList<>();
        table.getColumns().forEach(columns::add);

        // find the entity class associated with this database table
        Class<?> entityClass = findEntityClassForTable(table, metadata);

        if (entityClass != null) {
            // map each field name to its index appearance in the Java file
            Map<String, Integer> fieldOrderMap = new HashMap<>();
            int index = 0;
            // loop through the main entity fields
            for (Field field : entityClass.getDeclaredFields()) {
                
                // CRITICAL FIX : if the field is the Embedded ID, read those fields first
                if (field.isAnnotationPresent(EmbeddedId.class)) {
                    Class<?> embeddedIdClass = field.getType();
                    for (Field idField : embeddedIdClass.getDeclaredFields()) {
                        fieldOrderMap.put(idField.getName().toLowerCase(), index++);
                    }
                } else {
                    // regular fields
                    fieldOrderMap.put(field.getName().toLowerCase(), index++);
                }
            }

            // sort the DB columns
            columns.sort((col1, col2) -> {
                // primary key check : force PK to the top
                boolean isPk1 = table.getPrimaryKey() != null && table.getPrimaryKey().getColumns().contains(col1);
                boolean isPk2 = table.getPrimaryKey() != null && table.getPrimaryKey().getColumns().contains(col2);

                if (isPk1 && !isPk2) return -1; // col1 goes first
                if (!isPk1 && isPk2) return 1;  // col2 goes first

                // entity field order check (fallback if both are PK or both are regular columns)
                // remove underscores to handle camelCase to snake_case conversions safely
                Integer pos1 = fieldOrderMap.get(col1.getName().toLowerCase().replace("_", ""));
                Integer pos2 = fieldOrderMap.get(col2.getName().toLowerCase().replace("_", ""));

                if (pos1 != null && pos2 != null) {
                    return Integer.compare(pos1, pos2);
                }
                
                // fallback for system columns or implicit columns (like foreign keys)
                return Integer.compare(pos1 != null ? pos1 : 999, pos2 != null ? pos2 : 999);
            });
        }

        return columns;
    }

    private Class<?> findEntityClassForTable(Table table, Metadata metadata) {
        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            if (entityBinding.getTable().getName().equalsIgnoreCase(table.getName())) {
                return entityBinding.getMappedClass();
            }
        }
        return null;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.column_ordering_strategy", this);
    }

    @Override
    public List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata) {
        return new ArrayList<>(constraint.getColumns());
    }

    @Override
    public void orderTemporaryTableColumns(List<TemporaryTableColumn> columns, Metadata metadata) {
        // no-op (keep default order)
    }

    @Override
    public List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType type, Metadata metadata) {
        return new ArrayList<>(type.getColumns());
    }
}