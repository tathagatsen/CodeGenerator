package com.codegenerator.model_package_generator.model.dto;

import java.util.List;

public record DtoDefinition(
        String name,
        String sourceEntity,
        String type,
        List<DtoFieldDefinition> fields
) {}