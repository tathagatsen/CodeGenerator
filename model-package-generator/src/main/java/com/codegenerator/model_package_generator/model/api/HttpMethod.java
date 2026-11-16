package com.codegenerator.model_package_generator.model.api;

public enum HttpMethod {
    GET("GetMapping"),
    POST("PostMapping"),
    PUT("PutMapping"),
    PATCH("PatchMapping"),
    DELETE("DeleteMapping");

    private final String mappingAnnotation;

    HttpMethod(String mappingAnnotation) {
        this.mappingAnnotation = mappingAnnotation;
    }

    public String mappingAnnotation() {
        return mappingAnnotation;
    }
}
