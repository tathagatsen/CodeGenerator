# JSON-Driven Java Entity Generator

A Java-based code generation tool that reads entity and relationship definitions from a JSON schema and generates Java JPA entity source files.

The project aims to reduce repetitive entity creation by translating a structured JSON definition into Java classes with mapped fields and JPA relationships.

> **Project status:** Initial implementation. The generator is being developed incrementally; supported schema properties and relationship mappings should be checked against the current implementation.

## Overview

Instead of manually writing every entity class, define the database tables, columns, data types, and relationships in a JSON file. The generator processes that definition and creates corresponding Java source files.

### Generation flow

```text
JSON Schema
    |
    v
JSON Parser
    |
    v
Schema Validator
    |
    v
Data Type Mapper
    |
    v
Relationship Resolver
    |
    v
Entity Generator
    |
    v
Source File Writer
    |
    v
Generated Java Entity Files
```

Each component has a separate responsibility so that parsing, validation, type conversion, relationship resolution, code generation, and file writing can be maintained independently.

## Current components

| Component | Responsibility |
|---|---|
| JSON Parser | Reads the input JSON and converts it into the project's schema model. |
| `SchemaValidator` | Checks required properties, schema structure, field definitions, and supported configuration. |
| `DataTypeMapper` | Maps supported database data types to Java types and required imports. |
| `RelationshipResolver` | Resolves entity references and validates relationship mappings. |
| `EntityGenerator` | Builds Java entity source content from the validated schema. |
| `SourceFileWriter` | Writes generated source files to the configured output location. |

The exact supported features depend on the current implementation and its tests.

## Key goals

- Generate Java entity classes from JSON definitions.
- Keep JSON parsing and validation separate from code generation.
- Map supported database types to appropriate Java types.
- Resolve relationships between entities.
- Produce consistent and deterministic source files.
- Make it straightforward to add more data types, relationship types, and generation options.

## JSON schema

The generator uses a JSON document to describe the entities to generate.

A schema follows this general structure:

```json
{
    "schemaVersion": "1.0",
    "packageName": "com.example.entities",
    "tables": [
        {
            "name": "customer",
            "className": "Customer",
            "fields": [
                {
                    "name": "customer_id",
                    "type": "BIGINT",
                    "primaryKey": true,
                    "generated": true,
                    "nullable": false
                },
                {
                    "name": "customer_name",
                    "type": "VARCHAR",
                    "length": 100,
                    "nullable": false
                }
            ],
            "relationships": []
        }
    ]
}
```

### Main properties

| Property | Description |
|---|---|
| `schemaVersion` | Version of the JSON schema format. |
| `packageName` | Package name for generated Java classes. |
| `tables` | List of entity/table definitions. |
| `name` | Database table name. |
| `className` | Java entity class name. |
| `fields` | Database column definitions. |
| `relationships` | Relationships with other entities. |

For the complete authoring conventions, validation rules, type mappings, and relationship guidance, see **[JSON_SCHEMA_RULES.md](JSON_SCHEMA_RULES.md)**.

## Example: entity relationship

A table can declare a relationship to another entity. For example, an account can reference a customer through a `MANY_TO_ONE` relationship:

```json
{
    "name": "account",
    "className": "Account",
    "fields": [
        {
            "name": "account_id",
            "type": "BIGINT",
            "primaryKey": true,
            "generated": true,
            "nullable": false
        },
        {
            "name": "customer_id",
            "type": "BIGINT",
            "nullable": false
        }
    ],
    "relationships": [
        {
            "type": "MANY_TO_ONE",
            "field": "customer",
            "target": "Customer",
            "joinColumn": "customer_id",
            "referencedColumn": "customer_id",
            "optional": false,
            "fetch": "LAZY"
        }
    ]
}
```

The relationship definition must use property names supported by the current parser and relationship resolver. Refer to the schema rules document and implementation when preparing input.

## Requirements

- Java Development Kit (JDK): use the version configured by the project.
- Build tool: use the build system configured in the repository (for example, Maven or Gradle).
- A valid JSON schema following `JSON_SCHEMA_RULES.md`.

The exact JDK version, build command, and runtime entry point should be documented here once confirmed from the repository configuration.

## Getting started

1. Clone the repository:

   ```bash
   git clone <repository-url>
   ```

2. Open the project in your preferred Java IDE.

3. Ensure the configured JDK and build tool are installed.

4. Prepare a JSON input file following [`JSON_SCHEMA_RULES.md`](JSON_SCHEMA_RULES.md).

5. Configure the input JSON path and generated-source output path using the project's current entry point or configuration.

6. Run the application using the configured run configuration or build command.

> **Note:** The exact run command and configuration keys are intentionally not specified here because they must match the repository's actual build file and main application class.

## Validation and error handling

The intended processing order is:

1. Parse the JSON document.
2. Validate the schema structure and required properties.
3. Map database data types to Java types.
4. Resolve entity relationships.
5. Generate entity source content.
6. Write source files to the output directory.

Invalid input should be rejected before source files are written. Error messages should identify the relevant table, field, or relationship wherever possible.

## Development principles

- **Single responsibility:** each component should handle one stage of the generation pipeline.
- **Validate first:** do not generate files from an invalid schema.
- **Deterministic output:** identical input and generator versions should produce identical output.
- **Explicit mappings:** unsupported database types and relationship configurations should fail clearly.
- **Extensibility:** new mappings and relationship types should not require rewriting unrelated components.
- **Testability:** keep components independently testable and add end-to-end tests for the complete pipeline.

## Planned improvements

The following are potential next steps, not claims of completed functionality:

- Expand supported database data types and aliases.
- Add comprehensive validation for all relationship types.
- Support additional JPA annotations and column options.
- Add composite primary-key support.
- Add configurable naming strategies.
- Add command-line arguments for input and output paths.
- Add automated unit and integration tests.
- Add safe regeneration and overwrite handling.
- Add schema-version compatibility checks.

## Contributing

When extending the generator:

1. Update `JSON_SCHEMA_RULES.md` if the JSON contract changes.
2. Update the parser/model if new properties are introduced.
3. Add validation before using new schema properties.
4. Keep type mapping and relationship resolution separate from entity generation.
5. Add tests for valid, invalid, and boundary-case inputs.
6. Update this README when setup instructions, supported features, or usage change.

## License

No license has been specified yet. Add a `LICENSE` file and update this section before distributing the project.
