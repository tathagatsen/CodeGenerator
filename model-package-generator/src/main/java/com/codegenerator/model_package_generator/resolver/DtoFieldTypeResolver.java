
package com.codegenerator.model_package_generator.resolver;

import com.codegenerator.model_package_generator.mapper.DataTypeMapper;
import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.FieldDefinition;
import com.codegenerator.model_package_generator.model.ResolvedRelationship;
import com.codegenerator.model_package_generator.model.TableDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoFieldDefinition;

import java.util.List;
import java.util.Map;

public class DtoFieldTypeResolver {

    private final DataTypeMapper dataTypeMapper;

    public DtoFieldTypeResolver(DataTypeMapper dataTypeMapper) {
        if (dataTypeMapper == null) {
            throw new IllegalArgumentException(
                    "DataTypeMapper cannot be null."
            );
        }

        this.dataTypeMapper = dataTypeMapper;
    }

    public String resolve(
            DtoFieldDefinition dtoField,
            DtoDefinition dto,
            DatabaseSchema entitySchema,
            Map<String, List<ResolvedRelationship>> relationships
    ) {

        if (dtoField == null || dto == null || entitySchema == null) {
            throw new IllegalArgumentException(
                    "DTO field, DTO definition and entity schema cannot be null."
            );
        }

        if (dtoField.sourceField() != null &&
                !dtoField.sourceField().isBlank()) {

            return resolveSourceField(
                    dtoField,
                    dto,
                    entitySchema
            );
        }

        if (dtoField.sourceRelationship() != null &&
                !dtoField.sourceRelationship().isBlank()) {

            return resolveSourceRelationship(
                    dtoField,
                    dto,
                    entitySchema,
                    relationships
            );
        }

        throw new IllegalArgumentException(
                "No valid source defined for DTO field: " + dtoField.name()
        );
    }

    private String resolveSourceField(
            DtoFieldDefinition dtoField,
            DtoDefinition dto,
            DatabaseSchema entitySchema
    ) {

        TableDefinition sourceEntity = findEntity(
                entitySchema,
                dto.sourceEntity()
        );

        FieldDefinition sourceField = sourceEntity.fields()
                .stream()
                .filter(field ->
                        toJavaPropertyName(field.name())
                                .equals(dtoField.sourceField())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Source field not found: " +
                                        dtoField.sourceField()
                        )
                );

        return dataTypeMapper.getJavaType(sourceField.type())
                .getCanonicalName();
    }

    private String resolveSourceRelationship(
            DtoFieldDefinition dtoField,
            DtoDefinition dto,
            DatabaseSchema entitySchema,
            Map<String, List<ResolvedRelationship>> relationships
    ) {

        if (relationships == null) {
            throw new IllegalArgumentException(
                    "Resolved relationships cannot be null."
            );
        }

        TableDefinition sourceEntity = findEntity(
                entitySchema,
                dto.sourceEntity()
        );

        List<ResolvedRelationship> entityRelationships =
                relationships.get(sourceEntity.className());

        if (entityRelationships == null) {
            throw new IllegalArgumentException(
                    "No resolved relationships found for entity: " +
                            sourceEntity.className()
            );
        }

        ResolvedRelationship relationship = entityRelationships.stream()
                .filter(rel ->
                        rel.fieldName().equals(
                                dtoField.sourceRelationship()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Resolved relationship not found: " +
                                        dtoField.sourceRelationship() +
                                        " in entity " + sourceEntity.className()
                        )
                );

        TableDefinition targetEntity = findEntity(
                entitySchema,
                relationship.targetClass()
        );

        // Resolve the target entity's primary-key type.
        String javaType = resolvePrimaryKeyType(targetEntity, entitySchema);

        // Collection relationships return List<IDType>.
        if (isCollectionRelationship(relationship.relationshipType())) {
            return "java.util.List<" + javaType + ">";
        }

        return javaType;
    }

    private String resolvePrimaryKeyType(
            TableDefinition entity,
            DatabaseSchema entitySchema
    ) {

        List<FieldDefinition> primaryKeys = findPrimaryKeys(entity);

        if (primaryKeys.size() == 1) {

            // Single-column primary key.
            return dataTypeMapper
                    .getJavaType(primaryKeys.get(0).type())
                    .getCanonicalName();
        }

        // Composite primary key.
        if (primaryKeys.size() > 1) {
            return entitySchema.packageName()
                    + "."
                    + entity.className()
                    + "Id";
        }

        throw new IllegalArgumentException(
                "Primary key not found for entity: " +
                        entity.className()
        );
    }

    private TableDefinition findEntity(
            DatabaseSchema entitySchema,
            String entityName
    ) {

        return entitySchema.tables()
                .stream()
                .filter(table ->
                        table.className().equals(entityName)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Entity not found: " + entityName
                        )
                );
    }

    private List<FieldDefinition> findPrimaryKeys(
            TableDefinition entity
    ) {

        return entity.fields()
                .stream()
                .filter(field ->
                        Boolean.TRUE.equals(field.primaryKey())
                )
                .toList();
    }

    private boolean isCollectionRelationship(String relationshipType) {

        return "ONE_TO_MANY".equals(relationshipType) ||
                "MANY_TO_MANY".equals(relationshipType);
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
