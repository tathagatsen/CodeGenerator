package com.codegenerator.model_package_generator.generator;

import com.codegenerator.model_package_generator.model.api.ApiSchema;
import com.codegenerator.model_package_generator.model.api.ControllerDefinition;
import com.codegenerator.model_package_generator.model.api.EndpointDefinition;
import com.codegenerator.model_package_generator.model.api.PathVariableDefinition;
import com.codegenerator.model_package_generator.resolver.ApiTypeResolver;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiGenerator {

    private static final String WEB_ANNOTATIONS = "org.springframework.web.bind.annotation";
    private static final ClassName RESPONSE_ENTITY = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName MEDIA_TYPE = ClassName.get("org.springframework.http", "MediaType");
    private static final ClassName HTTP_STATUS = ClassName.get("org.springframework.http", "HttpStatus");
    private static final ClassName RESPONSE_STATUS_EXCEPTION =
            ClassName.get("org.springframework.web.server", "ResponseStatusException");
    private static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");

    private final ApiTypeResolver types;

    public ApiGenerator(ApiTypeResolver types) {
        this.types = types;
    }

    public Map<String, JavaFile> generate(ApiSchema schema) {
        ClassName apiResponse = ClassName.get(schema.responsePackage(), "ApiResponse");
        Map<String, Map<EndpointDefinition.Signature, EndpointDefinition>> services = new LinkedHashMap<>();
        Map<String, JavaFile> sources = new LinkedHashMap<>();
        for (ControllerDefinition controller : schema.controllers()) {
            addSource(sources, schema.controllerPackage(), generateController(schema, controller, apiResponse));
            for (EndpointDefinition endpoint : controller.endpoints()) {
                services.computeIfAbsent(endpoint.serviceClass(), name -> new LinkedHashMap<>())
                        .putIfAbsent(endpoint.signature(), endpoint);
            }
        }
        services.forEach((name, endpoints) ->
                addSource(sources, schema.servicePackage(), generateService(name, endpoints.values())));
        new ApiResponseGenerator().generate(apiResponse).forEach(file ->
                sources.put(file.packageName + "." + file.typeSpec.name, file));
        return sources;
    }

    private TypeSpec generateController(ApiSchema schema, ControllerDefinition definition, ClassName apiResponse) {
        TypeSpec.Builder controller = TypeSpec.classBuilder(definition.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(WEB_ANNOTATIONS, "RestController"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get(WEB_ANNOTATIONS, "RequestMapping"))
                        .addMember("path", "$S", definition.basePath())
                        .addMember("produces", "$T.APPLICATION_JSON_VALUE", MEDIA_TYPE)
                        .build());

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        Map<String, String> serviceFields = new LinkedHashMap<>();
        NameAllocator fieldNames = new NameAllocator();
        List<String> serviceClasses = definition.endpoints().stream().map(EndpointDefinition::serviceClass)
                .distinct().toList();
        for (String serviceClass : serviceClasses) {
            ClassName serviceType = ClassName.get(schema.servicePackage(), serviceClass);
            String fieldName = fieldNames.newName(Introspector.decapitalize(serviceClass));
            serviceFields.put(serviceClass, fieldName);
            controller.addField(FieldSpec.builder(serviceType, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
            constructor.addParameter(serviceType, fieldName);
            constructor.addStatement("this.$N = $N", fieldName, fieldName);
        }
        controller.addMethod(constructor.build());
        for (EndpointDefinition endpoint : definition.endpoints()) {
            controller.addMethod(generateEndpoint(endpoint, serviceFields.get(endpoint.serviceClass()), apiResponse));
        }
        return controller.build();
    }

    private MethodSpec generateEndpoint(EndpointDefinition endpoint, String serviceField, ClassName apiResponse) {
        AnnotationSpec mapping = AnnotationSpec.builder(
                        ClassName.get(WEB_ANNOTATIONS, endpoint.httpMethod().mappingAnnotation()))
                .addMember("value", "$S", endpoint.path())
                .build();

        List<ParameterSpec> parameters = parameters(endpoint, true);
        CodeBlock arguments = parameters.stream().map(parameter -> CodeBlock.of("$N", parameter))
                .collect(CodeBlock.joining(", "));
        CodeBlock serviceCall = CodeBlock.of("this.$N.$N($L)", serviceField, endpoint.methodName(), arguments);
        TypeName responseDto = responseType(endpoint);
        MethodSpec.Builder method = MethodSpec.methodBuilder(endpoint.methodName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(mapping)
                .addParameters(parameters)
                .returns(ParameterizedTypeName.get(RESPONSE_ENTITY,
                        ParameterizedTypeName.get(apiResponse, responseDto.box())));

        CodeBlock data = serviceCall;
        if (responseDto.equals(TypeName.VOID)) {
            method.addStatement("$L", serviceCall);
            data = CodeBlock.of("null");
        }
        return method.addStatement("return $T.status($L).body($T.success($S, $L))",
                        RESPONSE_ENTITY, endpoint.successStatus(), apiResponse, endpoint.successMessage(), data)
                .build();
    }

    private TypeSpec generateService(String name, Collection<EndpointDefinition> endpoints) {
        TypeSpec.Builder service = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(SERVICE);
        for (EndpointDefinition endpoint : endpoints) {
            service.addMethod(MethodSpec.methodBuilder(endpoint.methodName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(parameters(endpoint, false))
                    .returns(responseType(endpoint))
                    .addStatement("throw new $T($T.NOT_IMPLEMENTED)", RESPONSE_STATUS_EXCEPTION, HTTP_STATUS)
                    .build());
        }
        return service.build();
    }

    private List<ParameterSpec> parameters(EndpointDefinition endpoint, boolean controller) {
        List<ParameterSpec> parameters = new ArrayList<>();
        for (PathVariableDefinition variable : endpoint.pathVariables()) {
            ParameterSpec.Builder parameter = ParameterSpec.builder(types.pathVariableType(variable.javaType()),
                    variable.name());
            if (controller) {
                parameter.addAnnotation(AnnotationSpec.builder(ClassName.get(WEB_ANNOTATIONS, "PathVariable"))
                        .addMember("value", "$S", variable.name()).build());
            }
            parameters.add(parameter.build());
        }
        if (endpoint.requestDto() != null) {
            ParameterSpec.Builder request = ParameterSpec.builder(types.dtoType(endpoint.requestDto(), "REQUEST"),
                    "request");
            if (controller) {
                request.addAnnotation(ClassName.get(WEB_ANNOTATIONS, "RequestBody"));
            }
            parameters.add(request.build());
        }
        return parameters;
    }

    private TypeName responseType(EndpointDefinition endpoint) {
        return endpoint.responseDto() == null ? TypeName.VOID : types.dtoType(endpoint.responseDto(), "RESPONSE");
    }

    private void addSource(Map<String, JavaFile> sources, String packageName, TypeSpec type) {
        sources.put(packageName + "." + type.name, JavaFile.builder(packageName, type)
                .skipJavaLangImports(true).build());
    }
}
