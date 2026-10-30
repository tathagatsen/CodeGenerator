package com.codegenerator.model_package_generator.model;

import java.util.List;

public record DatabaseSchema(
        String schemaVersion,
        String packageName,
        List<TableDefinition> tables
) {
}
