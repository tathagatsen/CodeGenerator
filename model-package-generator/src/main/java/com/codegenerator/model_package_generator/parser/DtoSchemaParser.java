package com.codegenerator.model_package_generator.parser;

import com.codegenerator.model_package_generator.model.dto.DtoSchema;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

public class DtoSchemaParser {

    private final ObjectMapper objectMapper;

    public DtoSchemaParser() {
        this.objectMapper = new ObjectMapper();

        // Reject properties that are not defined in the DTO model records.
        this.objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                true
        );
    }

    public DtoSchema parse(Path filePath) throws IOException {

        if (filePath == null) {
            throw new IllegalArgumentException(
                    "DTO schema file path cannot be null."
            );
        }

        if (!java.nio.file.Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "DTO schema file does not exist: " + filePath
            );
        }

        DtoSchema dtoSchema = objectMapper.readValue(
                filePath.toFile(),
                DtoSchema.class
        );

        if (dtoSchema == null) {
            throw new IllegalArgumentException(
                    "DTO schema could not be parsed."
            );
        }

        return dtoSchema;
    }
}