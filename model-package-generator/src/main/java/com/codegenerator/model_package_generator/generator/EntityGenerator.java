package com.codegenerator.model_package_generator.generator;

import com.codegenerator.model_package_generator.mapper.DataTypeMapper;
import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.FieldDefinition;
import com.codegenerator.model_package_generator.model.ResolvedRelationship;
import com.codegenerator.model_package_generator.model.TableDefinition;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.*;

public class EntityGenerator {

    private static final ClassName ENTITY =
            ClassName.get("jakarta.persistence", "Entity");

    private static final ClassName TABLE =
            ClassName.get("jakarta.persistence", "Table");

    private static final ClassName ID =
            ClassName.get("jakarta.persistence", "Id");

    private static final ClassName GENERATED_VALUE =
            ClassName.get("jakarta.persistence", "GeneratedValue");

    private static final ClassName GENERATION_TYPE =
            ClassName.get("jakarta.persistence", "GenerationType");

    private static final ClassName COLUMN =
            ClassName.get("jakarta.persistence", "Column");

    private static final ClassName MANY_TO_ONE =
            ClassName.get("jakarta.persistence", "ManyToOne");

    private static final ClassName ONE_TO_ONE =
            ClassName.get("jakarta.persistence", "OneToOne");

    private static final ClassName ONE_TO_MANY =
            ClassName.get("jakarta.persistence", "OneToMany");

    private static final ClassName MANY_TO_MANY =
            ClassName.get("jakarta.persistence", "ManyToMany");

    private static final ClassName JOIN_COLUMN =
            ClassName.get("jakarta.persistence", "JoinColumn");

    private static final ClassName JOIN_TABLE =
            ClassName.get("jakarta.persistence", "JoinTable");

    private static final ClassName FETCH_TYPE =
            ClassName.get("jakarta.persistence", "FetchType");

    private final DataTypeMapper dataTypeMapper;

    public EntityGenerator(DataTypeMapper dataTypeMapper) {
        this.dataTypeMapper = dataTypeMapper;
    }

    public Map<String, JavaFile> generate(
            DatabaseSchema schema,
            Map<String, List<ResolvedRelationship>> relationships) {

        Map<String, JavaFile> generatedFiles = new java.util.LinkedHashMap<>();

        for (TableDefinition table : schema.tables()) {

            TypeSpec entityClass = buildEntity(
                    table,
                    relationships.getOrDefault(
                            table.className(),
                            List.of()
                    ),
                    schema.packageName()
            );

            JavaFile javaFile = JavaFile.builder(
                    schema.packageName(),
                    entityClass
            ).build();

            generatedFiles.put(table.className(), javaFile);
        }

        return Map.copyOf(generatedFiles);
    }

    private TypeSpec buildEntity(
            TableDefinition table,
            List<ResolvedRelationship> relationships,
            String packageName) {

        TypeSpec.Builder entityBuilder = TypeSpec
                .classBuilder(table.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ENTITY)
                .addAnnotation(
                        AnnotationSpec.builder(TABLE)
                                .addMember("name", "$S", table.name())
                                .build()
                );

        // Identify database columns already represented by relationships.
        Set<String> relationshipJoinColumns = new HashSet<>();

        for (ResolvedRelationship relationship : relationships) {
            if (relationship.owningSide()
                    && relationship.joinColumn() != null) {
                relationshipJoinColumns.add(
                        normalizeColumnName(relationship.joinColumn())
                );
            }
        }

        // Generate basic fields.
        if (table.fields() != null) {
            for (FieldDefinition field : table.fields()) {

                // Avoid generating customerId alongside Customer customer.
                if (relationshipJoinColumns.contains(
                        normalizeColumnName(field.name()))) {
                    continue;
                }

                entityBuilder.addField(buildBasicField(field));
            }
        }

        // Generate relationship fields.
        for (ResolvedRelationship relationship : relationships) {
            entityBuilder.addField(
                    buildRelationshipField(
                            relationship,
                            packageName
                    )
            );
        }

        return entityBuilder.build();
    }

    private FieldSpec buildBasicField(FieldDefinition field) {

        Class<?> javaType = dataTypeMapper.getJavaType(field.type());

        FieldSpec.Builder fieldBuilder = FieldSpec
                .builder(javaType, toCamelCase(field.name()))
                .addModifiers(Modifier.PRIVATE);

        // Primary key.
        if (Boolean.TRUE.equals(field.primaryKey())) {
            fieldBuilder.addAnnotation(ID);

            if (Boolean.TRUE.equals(field.generated())) {
                fieldBuilder.addAnnotation(
                        AnnotationSpec.builder(GENERATED_VALUE)
                                .addMember(
                                        "strategy",
                                        "$T.IDENTITY",
                                        GENERATION_TYPE
                                )
                                .build()
                );
            }
        }

        // Column annotation.
        AnnotationSpec.Builder columnBuilder =
                AnnotationSpec.builder(COLUMN)
                        .addMember("name", "$S", field.name());

        if (field.nullable() != null) {
            columnBuilder.addMember(
                    "nullable",
                    "$L",
                    field.nullable()
            );
        }

        if (Boolean.TRUE.equals(field.unique())) {
            columnBuilder.addMember("unique", "$L", true);
        }

        if (field.length() != null) {
            columnBuilder.addMember(
                    "length",
                    "$L",
                    field.length()
            );
        }

        if (field.precision() != null) {
            columnBuilder.addMember(
                    "precision",
                    "$L",
                    field.precision()
            );
        }

        if (field.scale() != null) {
            columnBuilder.addMember(
                    "scale",
                    "$L",
                    field.scale()
            );
        }

        fieldBuilder.addAnnotation(columnBuilder.build());

        return fieldBuilder.build();
    }

    private FieldSpec buildRelationshipField(
            ResolvedRelationship relationship,
            String packageName) {

        ClassName targetClass = ClassName.get(
                packageName,
                relationship.targetClass()
        );

        FieldSpec.Builder fieldBuilder;

        String fetchType = relationship.fetchType() == null
                ? "LAZY"
                : relationship.fetchType();

        switch (relationship.relationshipType()) {

            case "MANY_TO_ONE" -> {
                fieldBuilder = FieldSpec.builder(
                        targetClass,
                        relationship.fieldName(),
                        Modifier.PRIVATE
                );

                AnnotationSpec.Builder annotation =
                        AnnotationSpec.builder(MANY_TO_ONE)
                                .addMember(
                                        "fetch",
                                        "$T.$L",
                                        FETCH_TYPE,
                                        fetchType
                                )
                                .addMember(
                                        "optional",
                                        "$L",
                                        relationship.optional()
                                );

                fieldBuilder.addAnnotation(annotation.build());

                if (relationship.owningSide()) {
                    fieldBuilder.addAnnotation(
                            buildJoinColumn(relationship)
                    );
                }
            }

            case "ONE_TO_ONE" -> {
                fieldBuilder = FieldSpec.builder(
                        targetClass,
                        relationship.fieldName(),
                        Modifier.PRIVATE
                );

                AnnotationSpec.Builder annotation =
                        AnnotationSpec.builder(ONE_TO_ONE)
                                .addMember(
                                        "fetch",
                                        "$T.$L",
                                        FETCH_TYPE,
                                        fetchType
                                );

                if (relationship.mappedBy() != null) {
                    annotation.addMember(
                            "mappedBy",
                            "$S",
                            relationship.mappedBy()
                    );
                } else {
                    annotation.addMember(
                            "optional",
                            "$L",
                            relationship.optional()
                    );
                }

                fieldBuilder.addAnnotation(annotation.build());

                if (relationship.owningSide()) {
                    fieldBuilder.addAnnotation(
                            buildJoinColumn(relationship)
                    );
                }
            }

            case "ONE_TO_MANY" -> {
                fieldBuilder = buildCollectionField(
                        relationship,
                        targetClass
                );

                fieldBuilder.addAnnotation(
                        AnnotationSpec.builder(ONE_TO_MANY)
                                .addMember(
                                        "mappedBy",
                                        "$S",
                                        relationship.mappedBy()
                                )
                                .addMember(
                                        "fetch",
                                        "$T.$L",
                                        FETCH_TYPE,
                                        fetchType
                                )
                                .build()
                );
            }

            case "MANY_TO_MANY" -> {
                fieldBuilder = buildCollectionField(
                        relationship,
                        targetClass
                );

                AnnotationSpec.Builder annotation =
                        AnnotationSpec.builder(MANY_TO_MANY)
                                .addMember(
                                        "fetch",
                                        "$T.$L",
                                        FETCH_TYPE,
                                        fetchType
                                );

                if (relationship.owningSide()) {
                    fieldBuilder.addAnnotation(annotation.build());
                    fieldBuilder.addAnnotation(
                            buildJoinTable(relationship)
                    );
                } else {
                    annotation.addMember(
                            "mappedBy",
                            "$S",
                            relationship.mappedBy()
                    );
                    fieldBuilder.addAnnotation(annotation.build());
                }
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported relationship type: "
                            + relationship.relationshipType()
            );
        }

        return fieldBuilder.build();
    }

    private FieldSpec.Builder buildCollectionField(
            ResolvedRelationship relationship,
            ClassName targetClass) {

        TypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                targetClass
        );

        return FieldSpec.builder(
                        listType,
                        relationship.fieldName(),
                        Modifier.PRIVATE
                )
                .initializer("new $T<>()", ArrayList.class);
    }

    private AnnotationSpec buildJoinColumn(
            ResolvedRelationship relationship) {

        AnnotationSpec.Builder joinColumn =
                AnnotationSpec.builder(JOIN_COLUMN)
                        .addMember(
                                "name",
                                "$S",
                                relationship.joinColumn()
                        );

        if (relationship.referencedColumn() != null) {
            joinColumn.addMember(
                    "referencedColumnName",
                    "$S",
                    relationship.referencedColumn()
            );
        }

        joinColumn.addMember(
                "nullable",
                "$L",
                relationship.optional()
        );

        return joinColumn.build();
    }

    private AnnotationSpec buildJoinTable(
            ResolvedRelationship relationship) {

        AnnotationSpec joinColumn =
                AnnotationSpec.builder(JOIN_COLUMN)
                        .addMember(
                                "name",
                                "$S",
                                relationship.joinColumn()
                        )
                        .build();

        AnnotationSpec inverseJoinColumn =
                AnnotationSpec.builder(JOIN_COLUMN)
                        .addMember(
                                "name",
                                "$S",
                                relationship.inverseJoinColumn()
                        )
                        .build();

        return AnnotationSpec.builder(JOIN_TABLE)
                .addMember(
                        "name",
                        "$S",
                        relationship.joinTable()
                )
                .addMember(
                        "joinColumns",
                        "$L",
                        joinColumn
                )
                .addMember(
                        "inverseJoinColumns",
                        "$L",
                        inverseJoinColumn
                )
                .build();
    }

    private String normalizeColumnName(String name) {
        return name == null
                ? ""
                : name.replace("_", "").toLowerCase();
    }

    private String toCamelCase(String name) {

        String[] parts = name.toLowerCase().split("_");

        StringBuilder result = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(
                        Character.toUpperCase(parts[i].charAt(0))
                ).append(parts[i].substring(1));
            }
        }

        return result.toString();
    }
}