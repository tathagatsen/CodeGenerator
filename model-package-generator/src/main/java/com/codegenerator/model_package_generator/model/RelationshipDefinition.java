package com.codegenerator.model_package_generator.model;

public record RelationshipDefinition(

        String type,
        String field,
        String target,

        String joinColumn,
        String referencedColumn,

        String joinTable,
        String inverseJoinColumn,

        String inverseField,

        Boolean optional,
        String fetch,

        String mapsId

) {}