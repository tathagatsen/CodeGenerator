package com.codegenerator.model_package_generator.model.dto;

import java.util.List;
import java.util.Locale;

public record DtoSchema(
        String schemaVersion,
        String packageName,
        List<DtoDefinition> dtos
) {
    public String packageNameFor(DtoDefinition dto) {
        if (dto.type() == null) {
            throw new IllegalArgumentException("DTO type cannot be null.");
        }

        return switch (dto.type().toUpperCase(Locale.ROOT)) {
            case "REQUEST" -> packageName + ".request";
            case "RESPONSE" -> packageName + ".response";
            default -> throw new IllegalArgumentException("Unsupported DTO type: " + dto.type());
        };
    }
}
