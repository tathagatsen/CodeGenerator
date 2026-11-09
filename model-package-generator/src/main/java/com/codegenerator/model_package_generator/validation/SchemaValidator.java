package com.codegenerator.model_package_generator.validation;

import com.codegenerator.model_package_generator.mapper.DataTypeMapper;
import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.FieldDefinition;
import com.codegenerator.model_package_generator.model.RelationshipDefinition;
import com.codegenerator.model_package_generator.model.TableDefinition;

import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.regex.Pattern;

public class SchemaValidator {

    private static final Set<String> SUPPORTED_RELATIONSHIPS =
            Set.of("ONE_TO_ONE", "MANY_TO_ONE", "MANY_TO_MANY");

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile(
                    "^[a-zA-Z_$][a-zA-Z0-9_$]*" +
                            "(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*$"
            );

//    private final Set<String> supportedTypes;

    private final DataTypeMapper dataTypeMapper;

    public SchemaValidator(DataTypeMapper dataTypeMapper) {
        this.dataTypeMapper = dataTypeMapper;
    }
    public void validate(DatabaseSchema schema) {

        List<String> errors = new ArrayList<>();

        if (schema == null) {
            throw new SchemaValidationException(
                    List.of("Schema cannot be null.")
            );
        }

        // 1. Validate package name
        if (schema.packageName() == null ||
                !PACKAGE_PATTERN.matcher(schema.packageName()).matches()) {

            errors.add("Invalid Java package name: "
                    + schema.packageName());
        }

        // 2. Validate tables collection
        if (schema.tables() == null || schema.tables().isEmpty()) {
            errors.add("Schema must contain at least one table.");
            throwIfErrors(errors);
        }

        Map<String, TableDefinition> tablesByClass = new HashMap<>();
        Set<String> tableNames = new HashSet<>();

        // 3. Validate individual tables
        for (TableDefinition table : schema.tables()) {

            if (table == null) {
                errors.add("Table definition cannot be null.");
                continue;
            }

            if (isBlank(table.name())) {
                errors.add("Table name cannot be empty.");
                continue;
            }

            if (!tableNames.add(table.name().toLowerCase(Locale.ROOT))) {
                errors.add("Duplicate table name: " + table.name());
            }

            if (isBlank(table.className())) {
                errors.add("Table '" + table.name()
                        + "': Class name cannot be empty.");
                continue;
            }

            if (!SourceVersion.isIdentifier(table.className())
                    || SourceVersion.isKeyword(table.className())) {

                errors.add("Table '" + table.name()
                        + "': Invalid Java class name: "
                        + table.className());
            }

            if (tablesByClass.putIfAbsent(
                    table.className(), table) != null) {

                errors.add("Duplicate entity class name: "
                        + table.className());
            }

            validateFields(table, errors);
        }

        // 4. Validate relationships after all tables are indexed
        for (TableDefinition table : schema.tables()) {

            if (table == null || isBlank(table.name())) {
                continue;
            }

            validateRelationships(table, tablesByClass, errors);
        }

        throwIfErrors(errors);
    }


    private void validateFields(
            TableDefinition table,
            List<String> errors) {

        if (table.fields() == null || table.fields().isEmpty()) {
            errors.add("Table '" + table.name()
                    + "': At least one field is required.");
            return;
        }

        Set<String> fieldNames = new HashSet<>();
        List<FieldDefinition> primaryKeyFields = new ArrayList<>();

        for (FieldDefinition field : table.fields()) {

            if (field == null) {
                errors.add("Table '" + table.name()
                        + "': Field definition cannot be null.");
                continue;
            }

            if (isBlank(field.name())) {
                errors.add("Table '" + table.name()
                        + "': Field name cannot be empty.");
                continue;
            }

            if (!fieldNames.add(field.name().toLowerCase(Locale.ROOT))) {
                errors.add("Table '" + table.name()
                        + "': Duplicate field '" + field.name() + "'.");
            }

            if (!SourceVersion.isIdentifier(toCamelCase(field.name()))
                    || SourceVersion.isKeyword(toCamelCase(field.name()))) {

                errors.add("Table '" + table.name()
                        + "': Invalid Java field name: " + field.name());
            }

            if (!dataTypeMapper.isSupported(field.type())) {

                errors.add("Table '" + table.name()
                        + "': Unsupported datatype '"
                        + field.type() + "' for field '"
                        + field.name() + "'.");
            }

            // Collect primary-key fields
            if (Boolean.TRUE.equals(field.primaryKey())) {
                primaryKeyFields.add(field);

                if (Boolean.TRUE.equals(field.nullable())) {
                    errors.add("Table '" + table.name()
                            + "': Primary key field '" + field.name()
                            + "' cannot be nullable.");
                }
            }

            // Generated fields must be primary keys
            if (Boolean.TRUE.equals(field.generated())
                    && !Boolean.TRUE.equals(field.primaryKey())) {

                errors.add("Table '" + table.name()
                        + "': Generated field '" + field.name()
                        + "' must be a primary key.");
            }

            if (field.length() != null && field.length() <= 0) {
                errors.add("Table '" + table.name()
                        + "': Invalid length for field '"
                        + field.name() + "'.");
            }

            if (field.precision() != null && field.precision() <= 0) {
                errors.add("Table '" + table.name()
                        + "': Precision must be positive for field '"
                        + field.name() + "'.");
            }

            if (field.scale() != null && field.scale() < 0) {
                errors.add("Table '" + table.name()
                        + "': Scale cannot be negative for field '"
                        + field.name() + "'.");
            }

            if (field.precision() != null
                    && field.scale() != null
                    && field.scale() > field.precision()) {

                errors.add("Table '" + table.name()
                        + "': Scale cannot exceed precision for field '"
                        + field.name() + "'.");
            }
        }

        // Every table must have at least one primary key
        if (primaryKeyFields.isEmpty()) {
            errors.add("Table '" + table.name()
                    + "': No primary key defined.");
        }

        // Composite primary-key validation
        if (primaryKeyFields.size() > 1) {

            for (FieldDefinition primaryKey : primaryKeyFields) {

                if (Boolean.TRUE.equals(primaryKey.generated())) {
                    errors.add("Table '" + table.name()
                            + "': Auto-generated primary keys are not "
                            + "supported for composite keys. Field: "
                            + primaryKey.name());
                }
            }
        }
    }

    private void validateRelationships(
            TableDefinition source,
            Map<String, TableDefinition> tablesByClass,
            List<String> errors) {

        if (source.relationships() == null) {
            return;
        }

        Set<String> relationshipFields = new HashSet<>();
        Set<String> mapsIdFields = new HashSet<>();
        for (RelationshipDefinition relationship : source.relationships()) {

            if (relationship == null) {
                errors.add("Table '" + source.name()
                        + "': Relationship cannot be null.");
                continue;
            }

            String prefix = "Table '" + source.name() + "': ";

            if (isBlank(relationship.type())
                    || !SUPPORTED_RELATIONSHIPS.contains(
                    relationship.type().toUpperCase(Locale.ROOT))) {

                errors.add(prefix + "Unsupported relationship type: "
                        + relationship.type());
                continue;
            }

            if (isBlank(relationship.field())) {
                errors.add(prefix + "Relationship field cannot be empty.");
                continue;
            }

            String javaField = toCamelCase(relationship.field());

            if (!SourceVersion.isIdentifier(javaField)
                    || SourceVersion.isKeyword(javaField)) {
                errors.add(prefix + "Invalid relationship field: "
                        + relationship.field());
            }

            if (!relationshipFields.add(javaField.toLowerCase(Locale.ROOT))) {
                errors.add(prefix + "Duplicate relationship field: "
                        + javaField);
            }

            if (source.fields() != null &&
                    source.fields().stream()
                            .filter(Objects::nonNull)
                            .anyMatch(f -> f.name() != null &&
                                    toCamelCase(f.name())
                                            .equalsIgnoreCase(javaField))) {

                errors.add(prefix + "Relationship field '" + javaField
                        + "' conflicts with a database field.");
            }

            TableDefinition target =
                    tablesByClass.get(relationship.target());

            if (target == null) {
                errors.add(prefix + "Target entity '"
                        + relationship.target() + "' does not exist.");
                continue;
            }

            String type = relationship.type().toUpperCase(Locale.ROOT);

            validateMapsId(
                    source,
                    target,
                    relationship,
                    type,
                    mapsIdFields,
                    errors
            );

            // Relationships defined on the owning side.
            if ("MANY_TO_ONE".equals(type)
                    || "ONE_TO_ONE".equals(type)) {

                if (isBlank(relationship.joinColumn())) {
                    errors.add(prefix + type
                            + " requires a joinColumn.");
                } else if (!containsField(
                        source, relationship.joinColumn())) {

                    errors.add(prefix + "Join column '"
                            + relationship.joinColumn()
                            + "' does not exist in the source table.");
                }

                String referenced = relationship.referencedColumn();

                if (isBlank(referenced)) {
                    errors.add(prefix
                            + "referencedColumn must be specified.");
                } else if (!containsField(target, referenced)) {
                    errors.add(prefix + "Referenced column '"
                            + referenced + "' does not exist in target '"
                            + target.name() + "'.");
                }
            }

            if ("MANY_TO_MANY".equals(type)) {

                if (isBlank(relationship.joinTable())) {
                    errors.add(prefix
                            + "MANY_TO_MANY requires a joinTable.");
                }

                if (isBlank(relationship.joinColumn())) {
                    errors.add(prefix
                            + "MANY_TO_MANY requires a joinColumn.");
                }

                if (isBlank(relationship.inverseJoinColumn())) {
                    errors.add(prefix
                            + "MANY_TO_MANY requires an inverseJoinColumn.");
                }
            }
        }
    }

    private boolean containsField(
            TableDefinition table, String fieldName) {

        if (table.fields() == null) {
            return false;
        }

        return table.fields().stream()
                .filter(Objects::nonNull)
                .anyMatch(field -> field.name().equalsIgnoreCase(fieldName));
    }

    private String toCamelCase(String name) {

        if (name == null) return "";

        String[] parts = name.toLowerCase(Locale.ROOT).split("_");

        StringBuilder result = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                        .append(parts[i].substring(1));
            }
        }

        return result.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void throwIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new SchemaValidationException(errors);
        }
    }

    private void validateMapsId(
            TableDefinition source,
            TableDefinition target,
            RelationshipDefinition relationship,
            String type,
            Set<String> mapsIdFields,
            List<String> errors) {

        String prefix = "Table '" + source.name() + "': ";

        // No @MapsId mapping requested
        if (relationship.mapsId() == null) {
            return;
        }

        String mapsId = relationship.mapsId();

        if (isBlank(mapsId)) {
            errors.add(prefix + "mapsId cannot be empty.");
            return;
        }

        // Stage 1 supports only owning-side MANY_TO_ONE and ONE_TO_ONE.
        if (!"MANY_TO_ONE".equals(type)
                && !"ONE_TO_ONE".equals(type)) {

            errors.add(prefix
                    + "mapsId is supported only for MANY_TO_ONE "
                    + "and ONE_TO_ONE relationships.");

            return;
        }

        // Collect source primary-key fields.
        List<FieldDefinition> sourcePrimaryKeys =
                getPrimaryKeyFields(source);

        // @MapsId in this stage requires a composite child PK.
        if (sourcePrimaryKeys.size() <= 1) {
            errors.add(prefix
                    + "mapsId requires a composite primary key "
                    + "in the source entity.");
            return;
        }

        // Find the corresponding embedded ID component.
        FieldDefinition mappedIdField = sourcePrimaryKeys.stream()
                .filter(field ->
                        toCamelCase(field.name())
                                .equalsIgnoreCase(mapsId))
                .findFirst()
                .orElse(null);

        if (mappedIdField == null) {
            errors.add(prefix
                    + "mapsId '" + mapsId
                    + "' does not match any primary-key field "
                    + "in the source entity.");
            return;
        }

        // Ensure that the FK column and embedded ID component
        // represent the same database column.
        if (isBlank(relationship.joinColumn())
                || !mappedIdField.name().equalsIgnoreCase(
                relationship.joinColumn())) {

            errors.add(prefix
                    + "mapsId '" + mapsId
                    + "' must map to the same database column "
                    + "specified by joinColumn. Expected: "
                    + mappedIdField.name());
        }

        // Prevent multiple relationships from mapping
        // to the same embedded ID component.
        if (!mapsIdFields.add(mapsId.toLowerCase(Locale.ROOT))) {
            errors.add(prefix
                    + "Duplicate mapsId mapping: '" + mapsId + "'.");
        }

        // Stage 1 supports parent entities with single-column PKs.
        List<FieldDefinition> targetPrimaryKeys =
                getPrimaryKeyFields(target);

        if (targetPrimaryKeys.size() != 1) {
            errors.add(prefix
                    + "Stage 1 @MapsId supports only relationships "
                    + "to entities with a single-column primary key. "
                    + "Target: " + target.className());
            return;
        }

        FieldDefinition targetPrimaryKey = targetPrimaryKeys.get(0);

        // referencedColumn must point to the parent's PK.
        if (isBlank(relationship.referencedColumn())
                || !targetPrimaryKey.name().equalsIgnoreCase(
                relationship.referencedColumn())) {

            errors.add(prefix
                    + "For @MapsId, referencedColumn must reference "
                    + "the target entity's primary-key column: "
                    + targetPrimaryKey.name());
        }
    }

    private List<FieldDefinition> getPrimaryKeyFields(
            TableDefinition table) {

        if (table.fields() == null) {
            return Collections.emptyList();
        }

        return table.fields().stream()
                .filter(Objects::nonNull)
                .filter(field ->
                        Boolean.TRUE.equals(field.primaryKey()))
                .toList();
    }
}