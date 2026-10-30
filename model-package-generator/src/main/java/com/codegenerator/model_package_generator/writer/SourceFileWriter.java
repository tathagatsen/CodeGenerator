package com.codegenerator.model_package_generator.writer;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class SourceFileWriter {

    public void write(
            Map<String, JavaFile> generatedFiles,
            Path outputDirectory
    ) throws IOException {

        if (generatedFiles == null || generatedFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No generated Java files were provided."
            );
        }

        if (outputDirectory == null) {
            throw new IllegalArgumentException(
                    "Output directory cannot be null."
            );
        }

        // Create the output directory if it does not exist.
        java.nio.file.Files.createDirectories(outputDirectory);

        for (Map.Entry<String, JavaFile> entry
                : generatedFiles.entrySet()) {

            String entityName = entry.getKey();
            JavaFile javaFile = entry.getValue();

            if (javaFile == null) {
                throw new IllegalArgumentException(
                        "Generated JavaFile is null for entity: "
                                + entityName
                );
            }

            // JavaPoet creates the package structure and writes the file.
            javaFile.writeTo(outputDirectory);

            System.out.println(
                    "Generated: " + entityName + ".java"
            );
        }
    }
}