package com.codegenerator.model_package_generator;

import com.codegenerator.model_package_generator.generator.DtoGenerator;
import com.codegenerator.model_package_generator.generator.EntityGenerator;
import com.codegenerator.model_package_generator.generator.RepositoryGenerator;
import com.codegenerator.model_package_generator.mapper.DataTypeMapper;
import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.ResolvedRelationship;
import com.codegenerator.model_package_generator.model.dto.DtoDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoSchema;
import com.codegenerator.model_package_generator.parser.DtoSchemaParser;
import com.codegenerator.model_package_generator.parser.SchemaParser;
import com.codegenerator.model_package_generator.resolver.DtoFieldTypeResolver;
import com.codegenerator.model_package_generator.resolver.RelationshipResolver;
import com.codegenerator.model_package_generator.validation.DtoSchemaValidator;
import com.codegenerator.model_package_generator.validation.SchemaValidator;
import com.codegenerator.model_package_generator.writer.SourceFileWriter;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        try {

            // 1. Define input and output paths.

            Path entitySchemaPath = Path.of(
                    "schema",
                    "schema.json"
            );

            Path dtoSchemaPath = Path.of(
                    "schema",
                    "dto-schema.json"
            );

            Path outputPath = Path.of("generated");


            // 2. Parse entity schema.

            SchemaParser schemaParser = new SchemaParser();

            DatabaseSchema schema = schemaParser.parse(entitySchemaPath);

            System.out.println("Entity JSON parsing successful!");


            // 3. Parse DTO schema.

            DtoSchemaParser dtoSchemaParser = new DtoSchemaParser();

            DtoSchema dtoSchema = dtoSchemaParser.parse(dtoSchemaPath);

            System.out.println("DTO JSON parsing successful!");


            // 4. Initialize datatype mapper.

            DataTypeMapper dataTypeMapper = new DataTypeMapper();


            // 5. Validate entity schema.

            SchemaValidator schemaValidator =
                    new SchemaValidator(dataTypeMapper);

            schemaValidator.validate(schema);

            System.out.println("Entity schema validation successful!");


            // 6. Validate DTO schema against entity schema.

            DtoSchemaValidator dtoSchemaValidator =
                    new DtoSchemaValidator();

            dtoSchemaValidator.validate(dtoSchema, schema);

            System.out.println("DTO schema validation successful!");


            // 7. Resolve entity relationships.

            RelationshipResolver relationshipResolver =
                    new RelationshipResolver();

            Map<String, List<ResolvedRelationship>> relationships =
                    relationshipResolver.resolve(schema);

            System.out.println("\nRelationship resolution successful!");

            relationships.forEach((entity, relationshipList) -> {

                System.out.println("\nEntity: " + entity);

                relationshipList.forEach(relationship -> {

                    System.out.println(
                            relationship.relationshipType()
                                    + " -> " + relationship.targetClass()
                                    + " | Field: " + relationship.fieldName()
                                    + " | Owning: " + relationship.owningSide()
                                    + " | mappedBy: " + relationship.mappedBy()
                    );

                });
            });


            // 8. Generate JPA entity classes.

            EntityGenerator entityGenerator =
                    new EntityGenerator(dataTypeMapper);

            Map<String, JavaFile> generatedEntities =
                    entityGenerator.generate(schema, relationships);

            System.out.println("\nEntity generation successful!");


            // 9. Generate repository interfaces.

            RepositoryGenerator repositoryGenerator =
                    new RepositoryGenerator(dataTypeMapper);

            Map<String, JavaFile> generatedRepositories =
                    repositoryGenerator.generate(schema);

            System.out.println("\nRepository generation successful!");


            // 10. Initialize DTO field type resolver.

            DtoFieldTypeResolver dtoFieldTypeResolver =
                    new DtoFieldTypeResolver(dataTypeMapper);


            // 11. Generate DTO classes.

            DtoGenerator dtoGenerator =
                    new DtoGenerator(dtoFieldTypeResolver);

            Map<String, JavaFile> generatedDtos =
                    dtoGenerator.generate(
                            dtoSchema,
                            schema,
                            relationships
                    );

            System.out.println("\nDTO generation successful!");


            // 12. Display generated DTO definitions.

            for (DtoDefinition dto : dtoSchema.dtos()) {

                System.out.println(
                        "\nDTO: " + dto.name()
                                + " | Source Entity: " + dto.sourceEntity()
                                + " | Type: " + dto.type()
                );

            }


            // 13. Display generated Java files.

            System.out.println("\n========== GENERATED ENTITIES ==========");

            generatedEntities.forEach((name, javaFile) -> {

                System.out.println("\n========== " + name + " ==========");
                System.out.println(javaFile);

            });


            System.out.println("\n========== GENERATED REPOSITORIES ==========");

            generatedRepositories.forEach((name, javaFile) -> {

                System.out.println("\n========== " + name + " ==========");
                System.out.println(javaFile);

            });


            System.out.println("\n========== GENERATED DTOS ==========");

            generatedDtos.forEach((name, javaFile) -> {

                System.out.println("\n========== " + name + " ==========");
                System.out.println(javaFile);

            });


            // 14. Combine all generated Java files.

            Map<String, JavaFile> allGeneratedFiles =
                    new LinkedHashMap<>();

            allGeneratedFiles.putAll(generatedEntities);

            allGeneratedFiles.putAll(generatedRepositories);

            allGeneratedFiles.putAll(generatedDtos);


            // 15. Write all generated files to disk.

            SourceFileWriter sourceFileWriter =
                    new SourceFileWriter();

            sourceFileWriter.write(
                    allGeneratedFiles,
                    outputPath
            );

            System.out.println("\nAll source files written successfully!");


            // 16. Final summary.

            System.out.println("\n========== GENERATION SUMMARY ==========");

            System.out.println(
                    "Entities generated: " + generatedEntities.size()
            );

            System.out.println(
                    "Repositories generated: " + generatedRepositories.size()
            );

            System.out.println(
                    "DTOs generated: " + generatedDtos.size()
            );

            System.out.println(
                    "Total Java files generated: " + allGeneratedFiles.size()
            );

            System.out.println(
                    "\nAll checks passed! Code generation completed successfully."
            );


        } catch (IOException e) {

            System.err.println(
                    "File operation failed: " + e.getMessage()
            );

            e.printStackTrace();

        } catch (IllegalArgumentException e) {

            System.err.println(
                    "Invalid schema or configuration: " + e.getMessage()
            );

        } catch (Exception e) {

            System.err.println(
                    "Unexpected error: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}
