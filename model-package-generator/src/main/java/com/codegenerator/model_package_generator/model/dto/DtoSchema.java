package com.codegenerator.model_package_generator.model.dto;

import java.util.List;

public record DtoSchema(
        String schemaVersion,
        String packageName,
        List<DtoDefinition> dtos
) {}