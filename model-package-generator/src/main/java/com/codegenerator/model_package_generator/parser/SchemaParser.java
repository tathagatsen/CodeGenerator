package com.codegenerator.model_package_generator.parser;

import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

public class SchemaParser {

    private final ObjectMapper objectMapper;

    public SchemaParser() {
        this.objectMapper = new ObjectMapper();

        this.objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                true
        );
    }

    public DatabaseSchema parse(Path filePath) throws IOException {

        DatabaseSchema schema = objectMapper.readValue(
                filePath.toFile(),
                DatabaseSchema.class
        );

        if (schema == null) {
            throw new IllegalArgumentException(
                    "Schema file is empty or invalid."
            );
        }

        return schema;
    }
}