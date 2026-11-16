package com.codegenerator.model_package_generator.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.util.List;

class ApiResponseGenerator {

    private static final String SPRING_HTTP = "org.springframework.http";
    private static final String WEB_ANNOTATIONS = "org.springframework.web.bind.annotation";
    private static final ClassName RESPONSE_ENTITY = ClassName.get(SPRING_HTTP, "ResponseEntity");
    private static final ClassName HTTP_STATUS = ClassName.get(SPRING_HTTP, "HttpStatus");
    private static final ClassName HTTP_STATUS_CODE = ClassName.get(SPRING_HTTP, "HttpStatusCode");
    private static final ClassName HTTP_HEADERS = ClassName.get(SPRING_HTTP, "HttpHeaders");
    private static final ClassName MEDIA_TYPE = ClassName.get(SPRING_HTTP, "MediaType");
    private static final ClassName WEB_REQUEST = ClassName.get("org.springframework.web.context.request", "WebRequest");
    private static final ClassName EXCEPTION_HANDLER_BASE = ClassName.get(
            "org.springframework.web.servlet.mvc.method.annotation", "ResponseEntityExceptionHandler");

    List<JavaFile> generate(ClassName apiResponse) {
        return List.of(
                JavaFile.builder(apiResponse.packageName(), generateResponse(apiResponse)).skipJavaLangImports(true).build(),
                JavaFile.builder(apiResponse.packageName(), generateAdvice(apiResponse)).skipJavaLangImports(true).build()
        );
    }

    private TypeSpec generateResponse(ClassName apiResponse) {
        TypeVariableName payloadType = TypeVariableName.get("T");
        ParameterizedTypeName responseType = ParameterizedTypeName.get(apiResponse, payloadType);
        return TypeSpec.classBuilder(apiResponse.simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addTypeVariable(payloadType)
                .addField(TypeName.BOOLEAN, "success", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "message", Modifier.PRIVATE, Modifier.FINAL)
                .addField(payloadType, "data", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE)
                        .addParameter(TypeName.BOOLEAN, "success")
                        .addParameter(String.class, "message")
                        .addParameter(payloadType, "data")
                        .addStatement("this.success = success")
                        .addStatement("this.message = message")
                        .addStatement("this.data = data")
                        .build())
                .addMethod(MethodSpec.methodBuilder("success").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addTypeVariable(payloadType)
                        .returns(responseType)
                        .addParameter(String.class, "message")
                        .addParameter(payloadType, "data")
                        .addStatement("return new $T<>(true, message, data)", apiResponse)
                        .build())
                .addMethod(MethodSpec.methodBuilder("failure").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ParameterizedTypeName.get(apiResponse, ClassName.get(Void.class)))
                        .addParameter(String.class, "message")
                        .addStatement("return new $T<>(false, message, null)", apiResponse)
                        .build())
                .addMethod(getter("isSuccess", TypeName.BOOLEAN, "success"))
                .addMethod(getter("getMessage", TypeName.get(String.class), "message"))
                .addMethod(getter("getData", payloadType, "data"))
                .build();
    }

    private TypeSpec generateAdvice(ClassName apiResponse) {
        TypeName responseType = ParameterizedTypeName.get(RESPONSE_ENTITY, ClassName.get(Object.class));
        return TypeSpec.classBuilder("ApiExceptionHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get(WEB_ANNOTATIONS, "RestControllerAdvice"))
                .superclass(EXCEPTION_HANDLER_BASE)
                .addField(FieldSpec.builder(System.Logger.class, "LOGGER", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.getLogger(ApiExceptionHandler.class.getName())", System.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("createResponseEntity").addAnnotation(Override.class)
                        .addModifiers(Modifier.PROTECTED)
                        .returns(responseType)
                        .addParameter(Object.class, "body")
                        .addParameter(HTTP_HEADERS, "headers")
                        .addParameter(HTTP_STATUS_CODE, "status")
                        .addParameter(WEB_REQUEST, "request")
                        .addStatement("$T knownStatus = $T.resolve(status.value())", HTTP_STATUS, HTTP_STATUS)
                        .addStatement("String message = knownStatus != null ? knownStatus.getReasonPhrase() : $S", "Request failed")
                        .addStatement("return $T.status(status).headers(headers).contentType($T.APPLICATION_JSON)"
                                        + ".body($T.failure(message))", RESPONSE_ENTITY, MEDIA_TYPE, apiResponse)
                        .build())
                .addMethod(MethodSpec.methodBuilder("handleUnexpectedException").addModifiers(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSpec.builder(ClassName.get(WEB_ANNOTATIONS, "ExceptionHandler"))
                                .addMember("value", "$T.class", Exception.class).build())
                        .returns(responseType)
                        .addParameter(Exception.class, "exception")
                        .addParameter(WEB_REQUEST, "request")
                        .addStatement("LOGGER.log($T.ERROR, $S, exception)", System.Logger.Level.class,
                                "Unhandled API exception")
                        .addStatement("return handleExceptionInternal(exception, null, new $T(), "
                                + "$T.INTERNAL_SERVER_ERROR, request)", HTTP_HEADERS, HTTP_STATUS)
                        .build())
                .build();
    }

    private MethodSpec getter(String name, TypeName type, String field) {
        return MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC).returns(type)
                .addStatement("return $N", field).build();
    }
}
