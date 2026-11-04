package com.codegenerator.model_package_generator.validation;

import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.TableDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoFieldDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoSchema;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DtoSchemaValidator {

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*(\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*$");

    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

    private static final Set<String> SUPPORTED_DTO_TYPES =
            Set.of("REQUEST", "RESPONSE");

    public void validate(
            DtoSchema dtoSchema,
            DatabaseSchema entitySchema
    ) {

        if (dtoSchema == null) {
            throw new IllegalArgumentException(
                    "DTO schema cannot be null."
            );
        }

        if (entitySchema == null) {
            throw new IllegalArgumentException(
                    "Entity schema cannot be null."
            );
        }

        validatePackageName(dtoSchema.packageName());
        validateDtos(dtoSchema, entitySchema);
    }

    private void validatePackageName(String packageName) {

        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException(
                    "DTO package name cannot be empty."
            );
        }

        if (!PACKAGE_PATTERN.matcher(packageName).matches()) {
            throw new IllegalArgumentException(
                    "Invalid DTO package name: " + packageName
            );
        }
    }

    private void validateDtos(
            DtoSchema dtoSchema,
            DatabaseSchema entitySchema
    ) {

        if (dtoSchema.dtos() == null || dtoSchema.dtos().isEmpty()) {
            throw new IllegalArgumentException(
                    "DTO schema must contain at least one DTO."
            );
        }

        Set<String> dtoNames = new HashSet<>();

        for (DtoDefinition dto : dtoSchema.dtos()) {

            if (dto == null) {
                throw new IllegalArgumentException(
                        "DTO definition cannot be null."
                );
            }

            validateDtoName(dto.name());

            if (!dtoNames.add(dto.name())) {
                throw new IllegalArgumentException(
                        "Duplicate DTO name: " + dto.name()
                );
            }

            validateDtoType(dto.type());

            TableDefinition sourceEntity =
                    findEntity(entitySchema, dto.sourceEntity());

            validateFields(dto, sourceEntity);
        }
    }

    private void validateDtoName(String name) {

        if (name == null || !IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid DTO class name: " + name
            );
        }
    }

    private void validateDtoType(String type) {

        if (type == null ||
                !SUPPORTED_DTO_TYPES.contains(type.toUpperCase())) {

            throw new IllegalArgumentException(
                    "Unsupported DTO type: " + type +
                            ". Supported types: " + SUPPORTED_DTO_TYPES
            );
        }
    }

    private TableDefinition findEntity(
            DatabaseSchema entitySchema,
            String entityName
    ) {

        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException(
                    "DTO sourceEntity cannot be empty."
            );
        }

        if (entitySchema.tables() == null) {
            throw new IllegalArgumentException(
                    "Entity schema contains no tables."
            );
        }

        return entitySchema.tables()
                .stream()
                .filter(table ->
                        table != null &&
                                entityName.equals(table.className())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Source entity does not exist: " + entityName
                        )
                );
    }

    private void validateFields(
            DtoDefinition dto,
            TableDefinition sourceEntity
    ) {

        if (dto.fields() == null || dto.fields().isEmpty()) {
            throw new IllegalArgumentException(
                    "DTO must contain at least one field: " + dto.name()
            );
        }

        Set<String> fieldNames = new HashSet<>();

        for (DtoFieldDefinition field : dto.fields()) {

            if (field == null) {
                throw new IllegalArgumentException(
                        "DTO field cannot be null in: " + dto.name()
                );
            }

            validateFieldName(field.name(), dto.name());

            if (!fieldNames.add(field.name())) {
                throw new IllegalArgumentException(
                        "Duplicate field '" + field.name() +
                                "' in DTO: " + dto.name()
                );
            }

            validateFieldSource(field, dto, sourceEntity);
        }
    }

    private void validateFieldName(String fieldName, String dtoName) {

        if (fieldName == null ||
                !IDENTIFIER_PATTERN.matcher(fieldName).matches()) {

            throw new IllegalArgumentException(
                    "Invalid field name '" + fieldName +
                            "' in DTO: " + dtoName
            );
        }
    }

    private void validateFieldSource(
            DtoFieldDefinition field,
            DtoDefinition dto,
            TableDefinition sourceEntity
    ) {

        boolean hasSourceField =
                field.sourceField() != null &&
                        !field.sourceField().isBlank();

        boolean hasSourceRelationship =
                field.sourceRelationship() != null &&
                        !field.sourceRelationship().isBlank();

        if (hasSourceField == hasSourceRelationship) {
            throw new IllegalArgumentException(
                    "DTO field '" + field.name() +
                            "' in " + dto.name() +
                            " must specify exactly one of sourceField or sourceRelationship."
            );
        }

        if (hasSourceField) {
            validateSourceField(field, dto, sourceEntity);
        }

        if (hasSourceRelationship) {
            validateSourceRelationship(field, dto, sourceEntity);
        }
    }

    private void validateSourceField(
            DtoFieldDefinition dtoField,
            DtoDefinition dto,
            TableDefinition sourceEntity
    ) {

        boolean exists = sourceEntity.fields() != null &&
                sourceEntity.fields().stream()
                        .filter(field -> field != null)
                        .anyMatch(field ->
                                toJavaPropertyName(field.name())
                                        .equals(dtoField.sourceField())
                        );

        if (!exists) {
            throw new IllegalArgumentException(
                    "Source field '" + dtoField.sourceField() +
                            "' does not exist in entity '" +
                            dto.sourceEntity() + "' for DTO '" + dto.name() + "'."
            );
        }
    }

    private void validateSourceRelationship(
            DtoFieldDefinition dtoField,
            DtoDefinition dto,
            TableDefinition sourceEntity
    ) {

        boolean exists = sourceEntity.relationships() != null &&
                sourceEntity.relationships().stream()
                        .filter(relationship -> relationship != null)
                        .anyMatch(relationship ->
                                dtoField.sourceRelationship()
                                        .equals(relationship.field())
                        );

        if (!exists) {
            throw new IllegalArgumentException(
                    "Source relationship '" +
                            dtoField.sourceRelationship() +
                            "' does not exist in entity '" +
                            dto.sourceEntity() + "' for DTO '" + dto.name() + "'."
            );
        }
    }

    private String toJavaPropertyName(String databaseFieldName) {

        if (databaseFieldName == null || databaseFieldName.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean uppercaseNext = false;

        for (char character : databaseFieldName.toCharArray()) {

            if (character == '_') {
                uppercaseNext = true;
            } else {
                result.append(
                        uppercaseNext
                                ? Character.toUpperCase(character)
                                : character
                );
                uppercaseNext = false;
            }
        }

        return result.toString();
    }
}