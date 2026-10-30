package com.codegenerator.model_package_generator.model;

public record FieldDefinition(
        String name,
        String type,
        Boolean primaryKey,
        Boolean generated,
        Boolean nullable,
        Boolean unique,
        Integer length,
        Integer precision,
        Integer scale
) {}