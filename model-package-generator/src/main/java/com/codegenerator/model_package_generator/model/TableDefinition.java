package com.codegenerator.model_package_generator.model;

import java.util.List;

public record TableDefinition(
        String name,
        String className,
        List<FieldDefinition> fields,
        List<RelationshipDefinition> relationships
) {}