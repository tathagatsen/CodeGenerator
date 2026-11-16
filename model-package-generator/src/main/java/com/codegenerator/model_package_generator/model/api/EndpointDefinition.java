package com.codegenerator.model_package_generator.model.api;

import java.util.List;

public record EndpointDefinition(
        String methodName,
        HttpMethod httpMethod,
        String path,
        String serviceClass,
        List<PathVariableDefinition> pathVariables,
        String requestDto,
        String responseDto,
        Integer successStatus,
        String successMessage
) {
    public EndpointDefinition {
        pathVariables = pathVariables == null ? List.of() : pathVariables;
        successStatus = successStatus == null ? 200 : successStatus;
        successMessage = successMessage == null ? "Success" : successMessage;
    }

    public Signature signature() {
        return new Signature(methodName, pathVariables.stream().map(PathVariableDefinition::javaType).toList(),
                requestDto);
    }

    public record Signature(String methodName, List<String> pathVariableTypes, String requestDto) {}
}
