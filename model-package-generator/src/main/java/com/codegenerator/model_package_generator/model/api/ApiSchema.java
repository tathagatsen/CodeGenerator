package com.codegenerator.model_package_generator.model.api;

import java.util.List;

public record ApiSchema(
        String schemaVersion,
        String packageName,
        List<ControllerDefinition> controllers
) {
    public String controllerPackage() {
        return packageName + ".controller";
    }

    public String servicePackage() {
        return packageName + ".service";
    }

    public String responsePackage() {
        return packageName + ".api";
    }
}
