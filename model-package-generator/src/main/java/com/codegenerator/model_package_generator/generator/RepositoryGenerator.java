package com.codegenerator.model_package_generator.generator;

import com.codegenerator.model_package_generator.mapper.DataTypeMapper;
import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.FieldDefinition;
import com.codegenerator.model_package_generator.model.TableDefinition;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class RepositoryGenerator {

    private static final ClassName JPA_REPOSITORY =
            ClassName.get(
                    "org.springframework.data.jpa.repository",
                    "JpaRepository"
            );

    private static final ClassName REPOSITORY =
            ClassName.get(
                    "org.springframework.stereotype",
                    "Repository"
            );

    private final DataTypeMapper dataTypeMapper;

    public RepositoryGenerator(DataTypeMapper dataTypeMapper) {
        this.dataTypeMapper = dataTypeMapper;
    }

    public Map<String, JavaFile> generate(DatabaseSchema schema) {

        Map<String, JavaFile> generatedFiles = new LinkedHashMap<>();

        String repositoryPackage = getRepositoryPackage(
                schema.packageName()
        );

        for (TableDefinition table : schema.tables()) {

            FieldDefinition primaryKey = table.fields()
                    .stream()
                    .filter(field ->
                            Boolean.TRUE.equals(field.primaryKey()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Primary key not found for entity: "
                                            + table.className()
                            )
                    );

            Class<?> idJavaType =
                    dataTypeMapper.getJavaType(primaryKey.type());

            ClassName entityClass = ClassName.get(
                    schema.packageName(),
                    table.className()
            );

            ParameterizedTypeName repositoryType =
                    ParameterizedTypeName.get(
                            JPA_REPOSITORY,
                            entityClass,
                            com.squareup.javapoet.TypeName.get(idJavaType)
                    );

            TypeSpec repositoryInterface = TypeSpec
                    .interfaceBuilder(
                            table.className() + "Repository"
                    )
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(repositoryType)
                    .addAnnotation(REPOSITORY)
                    .build();

            JavaFile javaFile = JavaFile.builder(
                    repositoryPackage,
                    repositoryInterface
            ).build();

            generatedFiles.put(
                    table.className() + "Repository",
                    javaFile
            );
        }

        return Map.copyOf(generatedFiles);
    }

    private String getRepositoryPackage(String entityPackage) {

        if (entityPackage == null
                || !entityPackage.endsWith(".entity")) {
            throw new IllegalArgumentException(
                    "Entity package must end with '.entity'. "
                            + "Received: " + entityPackage
            );
        }

        return entityPackage.substring(
                0,
                entityPackage.length() - ".entity".length()
        ) + ".repository";
    }
}