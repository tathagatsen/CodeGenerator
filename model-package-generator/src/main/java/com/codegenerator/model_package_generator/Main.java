package com.codegenerator.model_package_generator;


import com.codegenerator.model_package_generator.generator.EntityGenerator;
import com.codegenerator.model_package_generator.mapper.DataTypeMapper;
import com.codegenerator.model_package_generator.model.DatabaseSchema;
import com.codegenerator.model_package_generator.model.ResolvedRelationship;
import com.codegenerator.model_package_generator.parser.SchemaParser;
import com.codegenerator.model_package_generator.resolver.RelationshipResolver;
import com.codegenerator.model_package_generator.validation.SchemaValidator;
import com.codegenerator.model_package_generator.writer.SourceFileWriter;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        try {
            // 1. Define input and output paths.
            Path inputPath = Path.of("D:\\Projects\\CodeGeneratorV1\\model-package-generator\\schema\\schema.json");
            Path outputPath = Path.of("generated");

            // 2. Parse JSON schema.
            SchemaParser schemaParser = new SchemaParser();

            DatabaseSchema schema = schemaParser.parse(inputPath);

            System.out.println("JSON parsing successful!");

            // 3. Initialize datatype mapper.
            DataTypeMapper dataTypeMapper = new DataTypeMapper();

            // 4. Validate schema.
            SchemaValidator schemaValidator =
                    new SchemaValidator(dataTypeMapper);

            schemaValidator.validate(schema);

            System.out.println("Schema validation successful!");

            // 5. Resolve entity relationships.
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

            // 6. Generate JPA entity classes.
            EntityGenerator entityGenerator =
                    new EntityGenerator(dataTypeMapper);

            Map<String, JavaFile> generatedEntities =
                    entityGenerator.generate(schema, relationships);

            System.out.println("\nEntity generation successful!");

            generatedEntities.forEach((entityName, javaFile) -> {

                System.out.println(
                        "\n========== " + entityName + ".java =========="
                );

                System.out.println(javaFile);
            });

            // 7. Write generated Java files to disk.
            SourceFileWriter sourceFileWriter =
                    new SourceFileWriter();

            sourceFileWriter.write(
                    generatedEntities,
                    outputPath
            );

            System.out.println(
                    "\nSource files written successfully!"
            );

            System.out.println(
                    "\nAll checks passed! Entity generation completed."
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
