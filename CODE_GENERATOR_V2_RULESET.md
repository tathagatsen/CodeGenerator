# Code Generator V2 — Complete Ruleset

**Scope:** Java 21 · Spring Data JPA · Entity + Repository + DTO generation  
**Specification:** V2

## 1. Purpose

Version 2 extends the original JSON-driven Java entity generator to generate:

- JPA entity classes.
- Spring Data repository interfaces.
- Request DTO classes.
- Response DTO classes.

V2 uses two independent JSON files:

- `schema/entity-schema.json` — database tables, columns, primary keys, and entity relationships.
- `schema/dto-schema.json` — the exact API-facing DTO classes and fields to generate.

The entity schema is the source of truth for persistence structure. The DTO schema is the source of truth for DTO structure and field selection. Do not merge these schemas.

## 2. Architecture and responsibilities

```text
entity-schema.json              dto-schema.json
        |                              |
   SchemaParser                 DtoSchemaParser
        |                              |
  SchemaValidator              DtoSchemaValidator
        |                              |
        +-----------+------------------+
                    |
             DataTypeMapper
                    |
           RelationshipResolver
                    |
       +------------+-------------+
       |            |             |
 EntityGenerator  RepositoryGenerator
       |            |             |
       +------------+-------------+
                    |
               DtoGenerator
                    |
              SourceFileWriter
                    |
             Generated Java files
```

| Component | Responsibility |
|---|---|
| `SchemaParser` | Parses entity JSON into entity schema models. |
| `SchemaValidator` | Validates entity schema structure, fields, keys, and relationships. |
| `DataTypeMapper` | Maps supported database types to Java types and imports. |
| `RelationshipResolver` | Resolves and validates entity relationships. |
| `EntityGenerator` | Generates JPA entity source code. |
| `RepositoryGenerator` | Generates Spring Data repository interfaces. |
| `DtoSchema` / DTO model classes | Represent DTO schema definitions. |
| `DtoSchemaParser` | Parses the separate DTO JSON. |
| `DtoSchemaValidator` | Validates DTO definitions and their references to entities. |
| `DtoFieldTypeResolver` | Derives DTO field types from entity fields and relationships. |
| `DtoGenerator` | Generates request and response DTO source code. |
| `SourceFileWriter` | Writes generated files to the output directory. |
| `Main` | Orchestrates the complete pipeline; it must not contain generator logic. |

Keep these responsibilities separate. Do not move parsing, validation, type mapping, relationship resolution, or file writing into the generators.

## 3. Processing order

The required dependency order is:

1. Parse `entity-schema.json`.
2. Validate the entity schema.
3. Map database field types.
4. Resolve entity relationships.
5. Parse `dto-schema.json`.
6. Validate DTO definitions against the resolved entity model.
7. Generate entities.
8. Generate repositories.
9. Generate DTOs.
10. Collect generated sources and detect duplicate output paths.
11. Write files through `SourceFileWriter`.

No source files should be written if a required parsing or validation stage fails.

---

# Part A — Entity schema

## 4. Entity schema root

The entity schema root must contain:

| Property | Required | Description |
|---|---:|---|
| `schemaVersion` | Yes | Supported entity schema version. |
| `packageName` | Yes | Base package for generated entities. |
| `tables` | Yes | Non-empty array of table/entity definitions. |

Example:

```json
{
    "schemaVersion": "1.0",
    "packageName": "com.example.generated.entity",
    "tables": []
}
```

Rules:

- `schemaVersion` must be supported.
- `packageName` must be a valid Java package.
- `tables` must be a non-empty array.
- Table names and entity class names must be unique.
- Reject unknown properties unless explicitly supported by the schema version.

## 5. Table/entity definitions

Each `tables` item represents one database table and one Java entity.

| Property | Required | Description |
|---|---:|---|
| `name` | Yes | Physical database table name. |
| `className` | Yes | Java entity class name. |
| `fields` | Yes | Database column definitions. |
| `relationships` | Yes / empty array | Entity relationship definitions. |

Rules:

- `name` must be non-empty.
- `className` must be a valid Java class identifier.
- `fields` must contain at least one field.
- `relationships` must be an array; use `[]` when there are no relationships.
- Table names and class names must be unique.
- Prefer `snake_case` for database tables and `PascalCase` for Java classes.

## 6. Field definitions

| Property | Required | Description |
|---|---:|---|
| `name` | Yes | Physical database column name. |
| `type` | Yes | Supported database type. |
| `primaryKey` | No | Whether this field is the primary key. |
| `generated` | No | Whether the primary key is generated automatically. |
| `nullable` | No | Whether the column permits NULL. |
| `unique` | No | Whether values must be unique. |
| `length` | Conditional | Length for supported string/binary types. |
| `precision` | Conditional | Numeric precision. |
| `scale` | Conditional | Numeric scale. |

Rules:

1. Every field needs a non-empty name and supported type.
2. Every table must have a primary key.
3. The initial contract supports a single-column primary key. Composite keys are out of scope until explicitly implemented.
4. `generated: true` is valid only for a primary key.
5. `length` is valid only for types that support it.
6. `precision` and `scale` are valid only for supported numeric types.
7. `scale` must be non-negative and no greater than `precision`.
8. Column names must be unique within a table.
9. Converted Java property names must be valid and collision-free.
10. JSON booleans must be actual `true`/`false` values, not strings.
11. Omitted properties must follow documented defaults; the generator must not guess.

Example:

```json
{
    "name": "customer_id",
    "type": "BIGINT",
    "primaryKey": true,
    "generated": true,
    "nullable": false
}
```

## 7. Database-to-Java type mapping

Keep this table synchronized with `DataTypeMapper`.

| Database type | Java type |
|---|---|
| `VARCHAR`, `CHAR`, `TEXT` | `String` |
| `BIGINT` | `Long` |
| `INTEGER`, `INT` | `Integer` |
| `SMALLINT`, `TINYINT` | `Short` |
| `DECIMAL`, `NUMERIC`, `NUMBER` | `BigDecimal` |
| `FLOAT`, `DOUBLE` | `Double` |
| `REAL` | `Float` |
| `BOOLEAN`, `BOOL`, `BIT` | `Boolean` |
| `DATE` | `LocalDate` |
| `TIME` | `LocalTime` |
| `TIMESTAMP` | `LocalDateTime` |
| `TIMESTAMP WITH TIME ZONE` | `OffsetDateTime` |
| `UUID` | `UUID` |
| `BINARY`, `VARBINARY`, `BLOB` | `byte[]` |
| `JSON`, `JSONB` | `String` (basic initial mapping) |

Rules:

- Type matching should be case-insensitive.
- Unsupported types must fail validation; never silently fall back to `String`.
- Generate required Java imports.
- Add database-specific aliases explicitly to the mapper.
- Test every supported mapping.

## 8. Entity relationships

Supported types:

- `ONE_TO_ONE`
- `ONE_TO_MANY`
- `MANY_TO_ONE`
- `MANY_TO_MANY`

Common relationship properties:

| Property | Required | Description |
|---|---:|---|
| `type` | Yes | Relationship type. |
| `field` | Yes | Java relationship property on the current entity. |
| `target` | Yes | Target entity class name. |
| `joinColumn` | Conditional | Foreign-key/join column. |
| `referencedColumn` | No | Target database column referenced by the join column. |
| `joinTable` | Conditional | Intermediate table for many-to-many. |
| `inverseJoinColumn` | Conditional | Join-table column referencing the target entity. |
| `mappedBy` or the agreed inverse-property key | Conditional | Property on the owning side. |
| `optional` | No | Whether the association is optional. |
| `fetch` | No | `LAZY` or `EAGER`. |

Rules:

1. `type`, `field`, and `target` are mandatory.
2. The target entity must exist in the entity schema.
3. Relationship field names must be valid Java properties.
4. A relationship field must not collide with a scalar field or another relationship.
5. `fetch`, when present, must be `LAZY` or `EAGER`.
6. Referenced columns must exist on the target entity.
7. Owning and inverse mappings must agree.
8. `MANY_TO_ONE` normally owns the foreign key.
9. `ONE_TO_MANY` normally references the owning-side property through `mappedBy`.
10. `ONE_TO_ONE` must identify its owning side and foreign-key mapping.
11. `MANY_TO_MANY` must define a join table and both join columns on the owning side.

**Contract compatibility:** Use the exact property names accepted by the current `RelationshipResolver`. Do not maintain competing aliases such as `mappedBy` and `inverseField` for the same concept unless a deliberate schema change is made across model, parser, validator, resolver, examples, and tests.

---

# Part B — DTO schema

## 9. Separate DTO schema

DTO definitions must be stored in `schema/dto-schema.json`, not embedded in the entity schema.

This separation allows:

- Multiple DTOs to be created from one entity.
- Request and response payloads to expose different fields.
- API models to differ from database structure.
- DTO validation and generation to evolve independently.

The DTO schema is the source of truth for which API-facing fields are generated.

## 10. DTO schema root

The root must contain:

| Property | Required | Description |
|---|---:|---|
| `schemaVersion` | Yes | Supported DTO schema version. |
| `packageName` | Yes | Base package for generated DTOs. |
| `dtos` | Yes | Array of DTO definitions. |

Example:

```json
{
    "schemaVersion": "1.0",
    "packageName": "com.example.generated.dto",
    "dtos": []
}
```

Rules:

- `schemaVersion` must be supported.
- `packageName` must be a valid Java package.
- `dtos` must be a non-empty array when DTO generation is requested.
- DTO names must be unique.
- Reject unknown properties unless explicitly supported.

## 11. DTO definitions

Each DTO object has:

| Property | Required | Description |
|---|---:|---|
| `name` | Yes | Complete Java DTO class name. |
| `sourceEntity` | Yes | Entity `className` from the entity schema. |
| `type` | Yes | `REQUEST` or `RESPONSE`. |
| `fields` | Yes | Ordered list of DTO fields. |

Rules:

1. `name` must be a valid Java class identifier.
2. `sourceEntity` must match an existing entity class.
3. `type` must be exactly `REQUEST` or `RESPONSE`.
4. `fields` must contain at least one field.
5. DTO names must be unique.
6. Only explicitly listed fields are generated. Never copy all entity fields automatically.
7. Preserve DTO field order from the JSON where practical.

## 12. DTO field definitions

Each DTO field must refer to exactly one source:

- `sourceField`: a scalar Java property on the source entity.
- `sourceRelationship`: a relationship property on the source entity.

| Property | Required | Description |
|---|---:|---|
| `name` | Yes | Generated DTO property name. |
| `sourceField` | Exactly one selector | Java property name of a scalar entity field. |
| `sourceRelationship` | Exactly one selector | Java property name of an entity relationship. |

Rules:

1. `name` must be a valid Java property identifier.
2. Exactly one of `sourceField` or `sourceRelationship` must be present.
3. Both selectors together are invalid.
4. Omitting both selectors is invalid.
5. `sourceField` must use the Java property name, not the database column name.
6. `sourceRelationship` must use the Java relationship property name.
7. The selected source must exist on `sourceEntity`.
8. DTO field names must be unique within the DTO.
9. DTO field names must not collide with generated members.
10. Arbitrary DTO type overrides are not part of the current V2 contract; types are inferred from the source.

### Scalar field example

If `Customer` has Java properties `customerId`, `customerName`, and `email`:

```json
{
    "name": "customerName",
    "sourceField": "customerName"
}
```

Use the Java property name, not the physical database column name such as `customer_name`.

### Relationship field example

If `Account` has a `customer` relationship to `Customer`:

```json
{
    "name": "customerId",
    "sourceRelationship": "customer"
}
```

Relationship fields are represented by related identifier values, not nested entity objects, in the current V2 design.

## 13. DTO relationship type resolution

The DTO field type is derived from the relationship cardinality and target entity primary-key Java type.

| Relationship | DTO field type |
|---|---|
| `MANY_TO_ONE` | Target primary-key Java type |
| `ONE_TO_ONE` | Target primary-key Java type |
| `ONE_TO_MANY` | `List<TargetPrimaryKeyType>` |
| `MANY_TO_MANY` | `List<TargetPrimaryKeyType>` |

Examples:

- If `Account.customer` is `MANY_TO_ONE` and `Customer.customerId` is `Long`, generate `Long customerId`.
- If `Customer.accounts` is `ONE_TO_MANY` and `Account.accountId` is `Long`, generate `List<Long> accountIds`.

Rules:

1. The target entity must exist.
2. The target entity must have a resolvable primary key.
3. Resolve the key's Java type using the same type mapping as entity generation.
4. Collection relationships use `List` with the correct generic type and import.
5. Do not expose a related entity object for these relationship selectors under the current V2 mapping.
6. If the target or key cannot be resolved, fail with a useful validation error.

## 14. DTO package and generated class rules

DTO package is determined by `type`:

| `type` | Generated package |
|---|---|
| `REQUEST` | `<packageName>.request` |
| `RESPONSE` | `<packageName>.response` |

For base package `com.example.generated.dto`:

- Request DTO: `com.example.generated.dto.request`
- Response DTO: `com.example.generated.dto.response`

Rules:

1. `name` is the complete class name; do not silently append or remove suffixes.
2. Derive package names consistently.
3. DTO classes must be public.
4. Generate only fields declared in the DTO JSON.
5. Generate getters and setters for each field, as required by the current `DtoGenerator`.
6. Generate required imports, including `java.util.List` for collection fields and relevant scalar imports.

## 15. DTO validation

`DtoSchemaValidator` must validate the DTO schema against the parsed, validated, and relationship-resolved entity model.

It must check:

- Required root properties and JSON value types.
- Valid DTO package name.
- Valid and unique DTO class names.
- Existing `sourceEntity`.
- `type` is `REQUEST` or `RESPONSE`.
- Non-empty DTO fields.
- Valid and unique DTO property names.
- Exactly one of `sourceField` and `sourceRelationship`.
- `sourceField` exists on the source entity.
- `sourceRelationship` exists on the source entity.
- Relationship targets exist.
- Target primary keys exist and their Java types can be resolved.
- Collection relationships resolve to `List<PK>`.
- Unsupported properties are rejected.

DTO validation must finish before DTO source files are written.

---

# Part C — Repository generation

## 16. Repository purpose and convention

Generate one Spring Data repository interface for every valid entity.

Recommended convention:

- Interface name: `<EntityClassName>Repository`
- Package: `<entity package>.repository`
- Base interface: `JpaRepository<EntityClassName, PrimaryKeyJavaType>`

Example:

```java
package com.example.generated.entity.repository;

import com.example.generated.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {
}
```

## 17. Repository rules

1. Generate one repository interface per entity.
2. Repository names must be unique.
3. Every repository must reference an existing generated entity.
4. The repository ID generic type must match the entity primary-key Java type.
5. Generate the correct entity and `JpaRepository` imports.
6. Do not invent custom finder methods.
7. Fail if an entity has no valid primary key.
8. Do not modify or duplicate entity definitions.
9. Generate interfaces, not implementation classes.
10. Write files under the package-derived directory.

The baseline V2 repository contract is `JpaRepository<Entity, ID>`. Custom finder methods, query annotations, projections, paging helpers, and custom repository implementations are out of scope unless explicitly added.

---

# Part D — Main orchestration and file output

## 18. Main orchestration rules

`Main` coordinates components and does not duplicate their internal logic.

It must:

1. Load both JSON files from configured paths.
2. Stop if either parser fails.
3. Validate the entity schema.
4. Resolve entity relationships.
5. Parse and validate the DTO schema against the resolved entity model.
6. Generate entities, repositories, and DTOs only after validation succeeds.
7. Collect generated sources using consistent output-path keys.
8. Detect duplicate output paths before writing.
9. Pass the combined generated sources to `SourceFileWriter`.
10. Report success only after writing completes.

## 19. Output directory and package layout

Generated files must follow Java package directory conventions.

Recommended structure:

```text
generated/
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── generated/
                        ├── entity/
                        │   ├── Customer.java
                        │   └── Account.java
                        ├── entity/
                        │   └── repository/
                        │       ├── CustomerRepository.java
                        │       └── AccountRepository.java
                        └── dto/
                            ├── request/
                            │   └── CreateCustomerDto.java
                            └── response/
                                └── CustomerResponseDto.java
```

This example assumes entity package `com.example.generated.entity`, repository package `com.example.generated.entity.repository`, and DTO base package `com.example.generated.dto`.

Rules:

1. Package declaration must match the directory structure.
2. Each public Java class/interface must be written to its own correctly named `.java` file.
3. Do not write if validation fails.
4. Detect duplicate target paths before writing.
5. Do not silently overwrite unrelated files.
6. Prefer staging generated output and completing the write only after generation succeeds.
7. Use UTF-8 encoding.
8. Output must be deterministic.

---

# Part E — Shared naming, JSON, and error rules

## 20. Naming conventions

| Element | Convention | Example |
|---|---|---|
| Database table | `snake_case` | `customer_account` |
| Database column | `snake_case` | `customer_name` |
| Entity class | `PascalCase` | `CustomerAccount` |
| Java property | `camelCase` | `customerName` |
| Relationship property | `camelCase` | `customer` |
| Repository | `PascalCase` + `Repository` | `CustomerRepository` |
| DTO class | `PascalCase` | `CustomerResponseDto` |
| DTO property | `camelCase` | `customerName` |
| Package | lowercase | `com.example.generated` |

Rules:

- Generated identifiers must be valid Java identifiers.
- Java reserved keywords must not be used.
- Name conversion must be deterministic.
- Detect collisions after conversion.
- Scalar fields and relationship properties must not collide.
- Generated class names must not collide within a package.

## 21. JSON formatting rules

1. Use double quotes for keys and string values.
2. Do not include comments or trailing commas.
3. Use JSON booleans and `null`, not quoted equivalents.
4. Use consistent indentation; four spaces are recommended.
5. Do not duplicate object property names.
6. Use the expected JSON type for every property.
7. Reject unknown properties unless explicitly supported.
8. Omit unused optional properties; use `null` only where explicitly allowed.
9. Preserve array order where meaningful, especially DTO field order.

## 22. Error handling

Errors should identify the schema file and the relevant table, field, relationship, DTO, or DTO field.

Examples:

```text
Entity schema: table 'account' references unknown target entity 'CustomerProfile'.

DTO schema: DTO 'AccountResponseDto' field 'customerId'
must define exactly one of 'sourceField' or 'sourceRelationship'.

DTO schema: DTO 'CustomerResponseDto' references unknown source field
'customerFullName' on entity 'Customer'.
```

Rules:

- Fail fast on invalid input.
- Do not silently ignore invalid properties.
- Do not silently substitute unsupported types.
- Do not write files after validation failure.
- Keep parsing, validation, resolution, generation, and writing errors distinguishable.
- Include the underlying cause in diagnostics where useful.

---

# Part F — Testing and acceptance criteria

## 23. Unit tests

Each component must be independently testable.

### Entity schema tests

Cover:

- Valid entity schema.
- Missing root properties.
- Invalid package name.
- Duplicate table or entity names.
- Empty fields.
- Missing primary key.
- Unsupported database type.
- Invalid length, precision, or scale.
- Duplicate database columns.
- Invalid Java property names.
- Unknown relationship target.
- Invalid join/referenced column.
- Inconsistent owning/inverse relationship mapping.

### DTO schema tests

Cover:

- Valid request DTO.
- Valid response DTO.
- Missing root properties.
- Invalid package name.
- Duplicate DTO names.
- Unknown source entity.
- Invalid DTO type.
- Empty DTO fields.
- Duplicate DTO field names.
- Both source selectors present.
- Neither source selector present.
- Unknown source field.
- Unknown source relationship.
- Target entity without a valid primary key.
- Correct scalar relationship ID type.
- Correct collection `List<PK>` type.

### Repository generator tests

Cover:

- One repository per entity.
- Correct entity generic type.
- Correct primary-key generic type.
- Correct package and imports.
- Invalid/missing primary key fails.
- Duplicate output path is detected.

### File writer tests

Cover:

- Package-to-directory conversion.
- UTF-8 output.
- Correct filenames.
- Duplicate output paths.
- No write after validation failure.
- Existing-file/overwrite policy.
- Failure does not leave inconsistent output where practical.

### End-to-end tests

At least one test must load both schemas and verify that entities, repositories, request DTOs, and response DTOs are generated into the expected packages and paths. Generated source should compile against the configured Java, JPA, and Spring Data dependencies.

## 24. V2 release acceptance checklist

- [ ] Entity schema is parsed and validated independently.
- [ ] DTO schema is parsed and validated independently.
- [ ] DTO references are checked against the validated entity model.
- [ ] Entity relationships are resolved before dependent DTO type derivation.
- [ ] Entities are generated with supported fields and JPA mappings.
- [ ] One Spring Data repository is generated per entity.
- [ ] Repository ID types match entity primary-key Java types.
- [ ] Request DTOs are generated under `.request`.
- [ ] Response DTOs are generated under `.response`.
- [ ] Only explicitly declared DTO fields are generated.
- [ ] Scalar DTO types derive from source entity fields.
- [ ] Relationship DTO fields derive from target primary-key types.
- [ ] Collection relationship DTO fields use `List<PK>`.
- [ ] DTO getters and setters are generated.
- [ ] All generated files are collected and written by `SourceFileWriter`.
- [ ] Invalid input prevents source writing.
- [ ] Generated output compiles against configured dependencies.
- [ ] Unit and end-to-end tests cover the supported contract.

---

# Part G — Out of scope until explicitly designed

Do not assume these features are supported merely because they may be useful:

- Composite primary keys (`@EmbeddedId`, `@IdClass`).
- Nested DTOs or nested object graphs.
- Arbitrary DTO field type overrides.
- DTO validation annotations (`@NotNull`, `@Size`, `@Email`, etc.).
- Required, editable, or read-only DTO metadata.
- Automatic entity-to-DTO or DTO-to-entity mapper generation.
- Custom repository finder methods and query generation.
- Pagination-specific repository methods.
- Lombok annotations.
- Java records instead of mutable DTO classes.
- OpenAPI/Swagger annotations.
- Database introspection or migration generation.
- Configurable constructors, builders, `equals`, or `hashCode`.
- Multiple database dialect profiles.
- Custom source templates.

These features require explicit schema design, implementation, and tests before being considered supported.

## 25. Change management

When changing the V2 JSON contract:

1. Update this ruleset.
2. Update the schema model.
3. Update the parser.
4. Update the validator.
5. Update type mapping or relationship resolution if affected.
6. Update the relevant generator.
7. Update sample JSON files.
8. Add or update unit and integration tests.
9. Update the README and release notes.
10. Increment the schema version for incompatible changes.

Never change a JSON property in only one layer. Documentation, model, parser, validator, resolver, generator, and tests must agree.

## 26. Final design principles

1. **Two schemas, two responsibilities:** entity schema defines persistence; DTO schema defines API payload shape.
2. **Explicit DTO selection:** only fields named in `dto-schema.json` are generated.
3. **Validate before generate:** all required schemas and references must be valid before generation.
4. **Resolve before deriving:** relationship-dependent DTO types are derived from resolved entity metadata.
5. **Consistent type resolution:** use one source of truth for database and primary-key Java types.
6. **Independent modules:** parsers, validators, resolvers, generators, and writers remain separate.
7. **Deterministic output:** identical schemas and generator versions produce identical files.
8. **No silent fallbacks:** unsupported or ambiguous input produces clear errors.
9. **Test the contract:** every programmatically enforceable rule should have a test.
10. **Keep documentation synchronized:** this file is the reference for V2 JSON authoring and implementation behavior.
