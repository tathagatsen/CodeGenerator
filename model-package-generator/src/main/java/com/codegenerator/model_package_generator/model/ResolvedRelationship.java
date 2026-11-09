package com.codegenerator.model_package_generator.model;

public record ResolvedRelationship(

        String sourceClass,
        String targetClass,

        String fieldName,
        String relationshipType,

        boolean owningSide,

        String mappedBy,

        String joinColumn,
        String referencedColumn,

        String joinTable,
        String inverseJoinColumn,

        boolean optional,
        String fetchType,

        String mapsId

) {}