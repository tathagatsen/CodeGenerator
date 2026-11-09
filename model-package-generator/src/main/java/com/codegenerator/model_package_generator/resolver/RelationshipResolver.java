package com.codegenerator.model_package_generator.resolver;

import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.RelationshipDefinition;
import com.codegenerator.model_package_generator.model.ResolvedRelationship;
import com.codegenerator.model_package_generator.model.TableDefinition;

import javax.lang.model.SourceVersion;
import java.util.*;

public class RelationshipResolver {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "MANY_TO_ONE",
            "ONE_TO_ONE",
            "MANY_TO_MANY"
    );

    private String toCamelCase(String name) {

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

    public Map<String, List<ResolvedRelationship>> resolve(
            DatabaseSchema schema) {

        Map<String, TableDefinition> tableIndex = new HashMap<>();

        // Index tables using their entity class names.
        for (TableDefinition table : schema.tables()) {

            tableIndex.put(table.className(), table);
        }

        // Initialize an empty relationship list for every entity.
        Map<String, List<ResolvedRelationship>> resolved = new LinkedHashMap<>();

        for (TableDefinition table : schema.tables()) {

            resolved.put(table.className(), new ArrayList<>());
        }

        // Process explicitly declared relationships.
        for (TableDefinition source : schema.tables()) {

            if (source.relationships() == null) {
                continue;
            }

            for (RelationshipDefinition relationship
                    : source.relationships()) {

                processRelationship(
                        source,
                        relationship,
                        tableIndex,
                        resolved
                );
            }
        }

        // Return immutable results.
        Map<String, List<ResolvedRelationship>> result =
                new LinkedHashMap<>();

        resolved.forEach((entity, relationships) ->
                result.put(entity, List.copyOf(relationships)));

        return Collections.unmodifiableMap(result);
    }

    private void processRelationship(
            TableDefinition source,
            RelationshipDefinition relationship,
            Map<String, TableDefinition> tableIndex,
            Map<String, List<ResolvedRelationship>> resolved) {

        String type = relationship.type().toUpperCase(Locale.ROOT);

        if (!SUPPORTED_TYPES.contains(type)) {

            throw new IllegalArgumentException(
                    "Relationship type '" + type
                            + "' cannot be used as an owning-side "
                            + "definition in V1. Table: "
                            + source.name()
            );
        }

        TableDefinition target =
                tableIndex.get(relationship.target());

        if (target == null) {

            throw new IllegalArgumentException(
                    "Target entity '" + relationship.target()
                            + "' does not exist."
            );
        }

        validateFieldName(
                relationship.field(),
                source.className()
        );

        // Determine default fetch behavior.
        String fetch = relationship.fetch() == null
                ? "LAZY"
                : relationship.fetch().toUpperCase(Locale.ROOT);

        if (!fetch.equals("LAZY") && !fetch.equals("EAGER")) {

            throw new IllegalArgumentException(
                    "Invalid fetch type '" + fetch
                            + "' in entity " + source.className()
            );
        }

        boolean optional = relationship.optional() == null
                || relationship.optional();

        // Generate owning-side relationship.
        ResolvedRelationship owningRelationship =
                new ResolvedRelationship(

                        source.className(),
                        target.className(),

                        relationship.field(),
                        type,

                        true,
                        null,

                        relationship.joinColumn(),
                        relationship.referencedColumn(),

                        relationship.joinTable(),
                        relationship.inverseJoinColumn(),

                        optional,
                        fetch,

                        relationship.mapsId()
                );

        addRelationship(
                resolved,
                source.className(),
                owningRelationship
        );

        // Generate inverse side only when explicitly requested.
        if (relationship.inverseField() != null
                && !relationship.inverseField().isBlank()) {

            createInverseRelationship(
                    source,
                    target,
                    relationship,
                    type,
                    resolved
            );
        }
    }

    private void createInverseRelationship(
            TableDefinition source,
            TableDefinition target,
            RelationshipDefinition definition,
            String owningType,
            Map<String, List<ResolvedRelationship>> resolved) {

        String inverseType;
        String inverseJoinColumn = null;

        switch (owningType) {

            case "MANY_TO_ONE" ->
                    inverseType = "ONE_TO_MANY";

            case "ONE_TO_ONE" ->
                    inverseType = "ONE_TO_ONE";

            case "MANY_TO_MANY" -> {
                inverseType = "MANY_TO_MANY";

                // On the inverse side, the join columns are reversed.
                inverseJoinColumn = definition.joinColumn();
            }

            default -> throw new IllegalStateException(
                    "Unexpected relationship type: " + owningType
            );
        }

        validateFieldName(
                definition.inverseField(),
                target.className()
        );
        boolean fieldExists = target.fields().stream()
                .filter(Objects::nonNull)
                .anyMatch(field ->
                        field.name() != null &&
                                toCamelCase(field.name())
                                        .equalsIgnoreCase(definition.inverseField())
                );

        if (fieldExists) {
            throw new IllegalArgumentException(
                    "Inverse relationship field '"
                            + definition.inverseField()
                            + "' conflicts with an existing field in "
                            + target.className()
            );
        }
        ResolvedRelationship inverseRelationship =
                new ResolvedRelationship(

                        target.className(),
                        source.className(),

                        definition.inverseField(),
                        inverseType,

                        false,
                        definition.field(),

                        null,
                        null,

                        null,
                        inverseJoinColumn,

                        true,
                        "LAZY",

                        null
                );
        addRelationship(
                resolved,
                target.className(),
                inverseRelationship
        );
    }

    private void addRelationship(
            Map<String, List<ResolvedRelationship>> resolved,
            String entityName,
            ResolvedRelationship relationship) {

        List<ResolvedRelationship> relationships =
                resolved.get(entityName);

        if (relationships == null) {
            throw new IllegalArgumentException(
                    "Unknown source entity: " + entityName
            );
        }

        boolean duplicate = relationships.stream()
                .anyMatch(existing ->
                        existing.fieldName().equalsIgnoreCase(
                                relationship.fieldName()
                        )
                );

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate relationship field '"
                            + relationship.fieldName()
                            + "' in entity " + entityName
            );
        }

        relationships.add(relationship);
    }

    private void validateFieldName(
            String fieldName,
            String entityName) {

        if (fieldName == null
                || !SourceVersion.isIdentifier(fieldName)
                || SourceVersion.isKeyword(fieldName)) {

            throw new IllegalArgumentException(
                    "Invalid relationship field '"
                            + fieldName + "' in entity "
                            + entityName
            );
        }
    }
}