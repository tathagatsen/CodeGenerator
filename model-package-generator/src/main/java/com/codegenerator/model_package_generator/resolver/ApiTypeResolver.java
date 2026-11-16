package com.codegenerator.model_package_generator.resolver;

import com.codegenerator.model_package_generator.model.dto.DtoDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoSchema;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiTypeResolver {

    private static final Map<String, TypeName> PATH_VARIABLE_TYPES = Stream.of(
            String.class, UUID.class, Long.class, Integer.class, Short.class, Byte.class,
            Double.class, Float.class, Boolean.class, Character.class, BigDecimal.class, BigInteger.class,
            long.class, int.class, short.class, byte.class, double.class, float.class, boolean.class, char.class
    ).collect(Collectors.toUnmodifiableMap(Class::getSimpleName, TypeName::get));

    private final DtoSchema dtoSchema;
    private final Map<String, DtoDefinition> dtos;

    public ApiTypeResolver(DtoSchema dtoSchema) {
        this.dtoSchema = dtoSchema;
        this.dtos = dtoSchema == null ? Map.of() : dtoSchema.dtos().stream()
                .collect(Collectors.toUnmodifiableMap(DtoDefinition::name, Function.identity()));
    }

    public TypeName pathVariableType(String javaType) {
        TypeName type = javaType == null ? null : PATH_VARIABLE_TYPES.get(javaType);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported path variable Java type '" + javaType + "'.");
        }
        return type;
    }

    public ClassName dtoType(String name, String expectedType) {
        DtoDefinition dto = dtos.get(name);
        if (dto == null) {
            throw new IllegalArgumentException("DTO '" + name + "' is missing from dto-schema.json.");
        }
        if (!expectedType.equalsIgnoreCase(dto.type())) {
            throw new IllegalArgumentException("DTO '" + name + "' must have type " + expectedType + ".");
        }
        return ClassName.get(dtoSchema.packageNameFor(dto), name);
    }
}
