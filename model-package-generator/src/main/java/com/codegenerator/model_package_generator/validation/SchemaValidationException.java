package com.codegenerator.model_package_generator.validation;

import java.util.List;

public class SchemaValidationException extends RuntimeException {

    private final List<String> errors;

    public SchemaValidationException(List<String> errors) {
        super("Schema validation failed:\n"
                + String.join("\n", errors));

        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}