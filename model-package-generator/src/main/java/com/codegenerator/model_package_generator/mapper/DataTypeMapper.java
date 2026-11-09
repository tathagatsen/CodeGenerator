package com.codegenerator.model_package_generator.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
public class DataTypeMapper {

    private final Map<String, Class<?>> typeMappings;

    public DataTypeMapper() {

        Map<String, Class<?>> mappings = new HashMap<>();

        // String types
        mappings.put("VARCHAR", String.class);
        mappings.put("CHAR", String.class);
        mappings.put("TEXT", String.class);
        mappings.put("CLOB", String.class);
        mappings.put("LONGVARCHAR", String.class);
        mappings.put("NVARCHAR", String.class);

        // Integer types
        mappings.put("BIGINT", Long.class);
        mappings.put("INTEGER", Integer.class);
        mappings.put("INT", Integer.class);
        mappings.put("SMALLINT", Short.class);
        mappings.put("TINYINT", Short.class);

        // Decimal and floating-point types
        mappings.put("DECIMAL", BigDecimal.class);
        mappings.put("NUMERIC", BigDecimal.class);
        mappings.put("NUMBER", BigDecimal.class);
        mappings.put("FLOAT", Double.class);
        mappings.put("DOUBLE", Double.class);
        mappings.put("REAL", Float.class);

        // Boolean
        mappings.put("BOOLEAN", Boolean.class);
        mappings.put("BOOL", Boolean.class);
        mappings.put("BIT", Boolean.class);

        // Date and time
        mappings.put("DATE", LocalDate.class);
        mappings.put("TIME", LocalTime.class);
        mappings.put("TIME WITH TIME ZONE", OffsetTime.class);
        mappings.put("TIMESTAMP", LocalDateTime.class);
        mappings.put("DATETIME", LocalDateTime.class);
        mappings.put("TIMESTAMP WITHOUT TIME ZONE",
                LocalDateTime.class);
        mappings.put("TIMESTAMP WITH TIME ZONE",
                OffsetDateTime.class);
        mappings.put("TIMESTAMPTZ", OffsetDateTime.class);

        // UUID
        mappings.put("UUID", UUID.class);

        // Binary types
        mappings.put("BINARY", byte[].class);
        mappings.put("VARBINARY", byte[].class);
        mappings.put("BLOB", byte[].class);
        mappings.put("LONGBLOB", byte[].class);

        // JSON types
        mappings.put("JSON", String.class);
        mappings.put("JSONB", String.class);

        // PostgreSQL-specific types
        mappings.put("TEXT[]", String[].class);
        mappings.put("INET", String.class);

        this.typeMappings = Collections.unmodifiableMap(mappings);
    }

    public Class<?> getJavaType(String databaseType) {

        if (databaseType == null || databaseType.isBlank()) {
            throw new IllegalArgumentException(
                    "Database datatype cannot be empty."
            );
        }

        String normalizedType = normalize(databaseType);

        Class<?> javaType = typeMappings.get(normalizedType);

        if (javaType == null) {
            throw new IllegalArgumentException(
                    "Unsupported database datatype: " + databaseType
            );
        }

        return javaType;
    }

    public Set<String> getSupportedTypes() {
        return typeMappings.keySet();
    }

    public boolean isSupported(String databaseType) {

        if (databaseType == null) {
            return false;
        }

        return typeMappings.containsKey(normalize(databaseType));
    }

    private String normalize(String databaseType) {

        return databaseType
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}