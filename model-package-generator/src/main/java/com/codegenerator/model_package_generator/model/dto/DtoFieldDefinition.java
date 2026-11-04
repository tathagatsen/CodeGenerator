package com.codegenerator.model_package_generator.model.dto;

public record DtoFieldDefinition(
        String name,
        String sourceField,
        String sourceRelationship
) {}