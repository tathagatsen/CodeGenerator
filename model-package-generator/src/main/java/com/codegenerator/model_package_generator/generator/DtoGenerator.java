package com.codegenerator.model_package_generator.generator;

import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.ResolvedRelationship;
import com.codegenerator.model_package_generator.model.dto.DtoDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoFieldDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoSchema;
import com.codegenerator.model_package_generator.resolver.DtoFieldTypeResolver;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DtoGenerator {

    private final DtoFieldTypeResolver dtoFieldTypeResolver;

    public DtoGenerator(DtoFieldTypeResolver dtoFieldTypeResolver) {

        if (dtoFieldTypeResolver == null) {
            throw new IllegalArgumentException(
                    "DtoFieldTypeResolver cannot be null."
            );
        }

        this.dtoFieldTypeResolver = dtoFieldTypeResolver;
    }

    public Map<String, JavaFile> generate(
            DtoSchema dtoSchema,
            DatabaseSchema entitySchema,
            Map<String, List<ResolvedRelationship>> relationships
    ) {

        if (dtoSchema == null || entitySchema == null) {
            throw new IllegalArgumentException(
                    "DTO schema and entity schema cannot be null."
            );
        }

        Map<String, JavaFile> generatedFiles = new LinkedHashMap<>();

        for (DtoDefinition dto : dtoSchema.dtos()) {

            String packageName = resolvePackageName(
                    dtoSchema.packageName(),
                    dto.type()
            );

            TypeSpec dtoClass = generateDtoClass(
                    dto,
                    entitySchema,
                    relationships
            );

            JavaFile javaFile = JavaFile.builder(
                    packageName,
                    dtoClass
            ).build();

            String fileKey = packageName + "." + dto.name();

            if (generatedFiles.putIfAbsent(fileKey, javaFile) != null) {
                throw new IllegalArgumentException(
                        "Duplicate generated DTO class: " + fileKey
                );
            }
        }

        return generatedFiles;
    }

    private TypeSpec generateDtoClass(
            DtoDefinition dto,
            DatabaseSchema entitySchema,
            Map<String, List<ResolvedRelationship>> relationships
    ) {

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(dto.name())
                .addModifiers(Modifier.PUBLIC);

        for (DtoFieldDefinition dtoField : dto.fields()) {

            String resolvedType = dtoFieldTypeResolver.resolve(
                    dtoField,
                    dto,
                    entitySchema,
                    relationships
            );

            TypeName fieldType = resolveTypeName(resolvedType);

            FieldSpec fieldSpec = FieldSpec.builder(
                    fieldType,
                    dtoField.name(),
                    Modifier.PRIVATE
            ).build();

            classBuilder.addField(fieldSpec);

            classBuilder.addMethod(
                    generateGetter(dtoField.name(), fieldType)
            );

            classBuilder.addMethod(
                    generateSetter(dtoField.name(), fieldType)
            );
        }

        return classBuilder.build();
    }

    private MethodSpec generateGetter(
            String fieldName,
            TypeName fieldType
    ) {

        String methodName = "get" + capitalize(fieldName);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return $N", fieldName)
                .build();
    }

    private MethodSpec generateSetter(
            String fieldName,
            TypeName fieldType
    ) {

        String methodName = "set" + capitalize(fieldName);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }

    private String resolvePackageName(
            String basePackage,
            String dtoType
    ) {

        if (dtoType == null) {
            throw new IllegalArgumentException(
                    "DTO type cannot be null."
            );
        }

        return switch (dtoType.toUpperCase()) {
            case "REQUEST" -> basePackage + ".request";
            case "RESPONSE" -> basePackage + ".response";
            default -> throw new IllegalArgumentException(
                    "Unsupported DTO type: " + dtoType
            );
        };
    }

    private TypeName resolveTypeName(String javaType) {

        if (javaType == null || javaType.isBlank()) {
            throw new IllegalArgumentException(
                    "Resolved Java type cannot be empty."
            );
        }

        // Handle List<T> types.
        if (javaType.startsWith("java.util.List<")
                && javaType.endsWith(">")) {

            String elementType = javaType.substring(
                    "java.util.List<".length(),
                    javaType.length() - 1
            );

            return ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    resolveTypeName(elementType)
            );
        }

        // Handle primitive types.
        return switch (javaType) {
            case "boolean" -> TypeName.BOOLEAN;
            case "byte" -> TypeName.BYTE;
            case "short" -> TypeName.SHORT;
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "float" -> TypeName.FLOAT;
            case "double" -> TypeName.DOUBLE;
            case "char" -> TypeName.CHAR;
            case "byte[]" -> TypeName.get(byte[].class);
            default -> ClassName.bestGuess(javaType);
        };
    }

    private String capitalize(String fieldName) {

        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }

        return Character.toUpperCase(fieldName.charAt(0))
                + fieldName.substring(1);
    }
}