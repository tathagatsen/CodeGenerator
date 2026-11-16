package com.codegenerator.model_package_generator.model.api;

import java.util.List;

public record ControllerDefinition(
        String className,
        String basePath,
        List<EndpointDefinition> endpoints
) {}
