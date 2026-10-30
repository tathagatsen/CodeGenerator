# JSON Schema Authoring Ruleset

**Project:** JSON-Driven Java Entity Generator\
**Version:** 1.0\
**Target:** Java 21 · JPA · JSON

## 1. Purpose

This document defines the rules every JSON schema must follow when used
as input to the Java Entity Generator.

The goals are to ensure that JSON inputs are:

-   Structurally consistent and unambiguous.
-   Validated before code generation.
-   Mappable to supported Java types.
-   Suitable for resolving JPA relationships.
-   Capable of producing deterministic, compilable entity classes.

The JSON schema is the source of truth for generated entities.

## 2. Root-level structure

Every JSON document must contain these properties:

  Property            Required Description
  ----------------- ---------- -----------------------------------------
  `schemaVersion`          Yes Version of the input schema format.
  `packageName`            Yes Java package for generated entities.
  `tables`                 Yes Collection of table/entity definitions.

Rules:

1.  `schemaVersion` must be a supported version.
2.  `packageName` must follow Java package naming conventions.
3.  `tables` must be a non-empty array.
4.  Table names and entity class names must be unique within the
    document.
5.  Unknown root-level properties must be rejected unless explicitly
    supported by a later schema version.

Example:

``` json
{
    "schemaVersion": "1.0",
    "packageName": "com.example.entities",
    "tables": []
}
```

## 3. Table definition

Every object in `tables` must follow this structure:

  ------------------------------------------------------------------------
  Property                                  Required Description
  --------------------- ---------------------------- ---------------------
  `name`                                         Yes Physical database
                                                     table name.

  `className`                                    Yes Java entity class
                                                     name.

  `fields`                                       Yes Database column
                                                     definitions.

  `relationships`                                 No JPA relationship
                                                     definitions; defaults
                                                     to an empty
                                                     collection if
                                                     omitted, if supported
                                                     by the
                                                     implementation.
  ------------------------------------------------------------------------

Rules:

1.  `name` must be non-empty.
2.  `className` must be a valid Java class identifier.
3.  `fields` must contain at least one field.
4.  Duplicate table names are not allowed.
5.  Duplicate entity class names are not allowed.
6.  Database table names should use `snake_case`.
7.  Java class names must use `PascalCase`.
8.  A table's `relationships` property must be an array when present.

Example:

``` json
{
    "name": "customer",
    "className": "Customer",
    "fields": [],
    "relationships": []
}
```

## 4. Field definition

Each field represents a database column.

  ------------------------------------------------------------------------
  Property                                  Required Description
  --------------------- ---------------------------- ---------------------
  `name`                                         Yes Physical database
                                                     column name.

  `type`                                         Yes Supported database
                                                     data type.

  `primaryKey`                                    No Whether the field is
                                                     part of the primary
                                                     key.

  `generated`                                     No Whether the primary
                                                     key is generated
                                                     automatically.

  `nullable`                                      No Whether the column
                                                     permits `NULL`.

  `unique`                                        No Whether the column
                                                     has a uniqueness
                                                     constraint.

  `length`                               Conditional Maximum length for
                                                     supported
                                                     string/binary types.

  `precision`                            Conditional Numeric precision for
                                                     supported numeric
                                                     types.

  `scale`                                Conditional Numeric scale for
                                                     supported numeric
                                                     types.
  ------------------------------------------------------------------------

Rules:

1.  Every field must have a non-empty `name` and a supported `type`.
2.  Each table must have at least one primary key.
3.  Version 1.0 supports a single-column primary key only. Composite
    primary keys require an explicit future feature.
4.  `generated: true` is valid only for a primary key.
5.  `length` may be used only with types that support a length.
6.  `precision` and `scale` may be used only with supported numeric
    types.
7.  `scale` must be greater than or equal to zero and must not exceed
    `precision`.
8.  Duplicate database column names are not allowed within a table.
9.  Field names must be convertible to valid, non-conflicting Java
    property names.
10. Boolean properties must contain JSON booleans, not strings such as
    `"true"` or `"false"`.
11. Defaults should be defined by the schema specification rather than
    guessed by the generator. Where a property is omitted, the validator
    and generator must apply one documented default consistently.

Example:

``` json
{
    "name": "customer_id",
    "type": "BIGINT",
    "primaryKey": true,
    "generated": true,
    "nullable": false
}
```

## 5. Database-to-Java type mapping

Only explicitly supported database types may be accepted. The following
is the proposed version 1.0 mapping:

  Database type                    Java type
  -------------------------------- ----------------------------------
  `VARCHAR`, `CHAR`, `TEXT`        `String`
  `BIGINT`                         `Long`
  `INTEGER`, `INT`                 `Integer`
  `SMALLINT`, `TINYINT`            `Short`
  `DECIMAL`, `NUMERIC`, `NUMBER`   `BigDecimal`
  `FLOAT`, `DOUBLE`                `Double`
  `REAL`                           `Float`
  `BOOLEAN`, `BOOL`, `BIT`         `Boolean`
  `DATE`                           `LocalDate`
  `TIME`                           `LocalTime`
  `TIMESTAMP`                      `LocalDateTime`
  `TIMESTAMP WITH TIME ZONE`       `OffsetDateTime`
  `UUID`                           `UUID`
  `BINARY`, `VARBINARY`, `BLOB`    `byte[]`
  `JSON`, `JSONB`                  `String` (initial basic mapping)

Rules:

1.  Type matching should be case-insensitive, with whitespace normalized
    where appropriate.
2.  Unsupported types must cause validation failure. They must not
    silently fall back to `String`.
3.  Required Java imports must be resolved by the generator.
4.  `BigDecimal` fields should specify precision and scale when those
    values are known or required by the database definition.
5.  Database-specific type aliases must be explicitly added to the
    mapper before they are accepted.
6.  The initial `JSON`/`JSONB` to `String` mapping is a basic
    representation; richer JSON mappings require an explicit future
    configuration.

## 6. Relationship definitions

Relationships must be explicitly declared in a table's `relationships`
array.

Supported relationship types:

-   `ONE_TO_ONE`
-   `ONE_TO_MANY`
-   `MANY_TO_ONE`
-   `MANY_TO_MANY`

Proposed relationship properties:

  ------------------------------------------------------------------------
  Property                                  Required Description
  --------------------- ---------------------------- ---------------------
  `type`                                         Yes Relationship type.

  `field`                                        Yes Java relationship
                                                     property name on the
                                                     current entity.

  `target`                                       Yes Target entity class
                                                     name.

  `joinColumn`                           Conditional Foreign-key column
                                                     used by the owning
                                                     side.

  `referencedColumn`                              No Target column
                                                     referenced by the
                                                     join column.

  `joinTable`                            Conditional Intermediate table
                                                     for a many-to-many
                                                     relationship.

  `inverseJoinColumn`                    Conditional Join column pointing
                                                     to the target entity
                                                     in a join table.

  `mappedBy`                             Conditional Relationship property
                                                     on the owning entity,
                                                     for an inverse
                                                     relationship.

  `optional`                                      No Whether the
                                                     association is
                                                     optional.

  `fetch`                                         No Fetch strategy:
                                                     `LAZY` or `EAGER`.
  ------------------------------------------------------------------------

**Implementation compatibility note:** Property names must match the
actual model classes and the current `RelationshipResolver`. If the
implementation currently uses a different property (for example,
`mappedBy` or `inverseField`), standardize the JSON contract and update
the parser, validator, resolver, examples, and tests together. Do not
support two aliases indefinitely without a documented reason.

### 6.1 General relationship rules

1.  `type`, `field`, and `target` are mandatory.
2.  The target entity must exist in the same schema.
3.  `field` must be a valid Java identifier and must not conflict with
    another field or relationship property in the entity.
4.  Relationship types must be one of the four supported uppercase
    values.
5.  `fetch`, when supplied, must be `LAZY` or `EAGER`.
6.  `mappedBy`, when supplied, must name a valid relationship property
    on the target/owning entity, according to the selected relationship
    direction.
7.  Every referenced entity property and database column must exist.
8.  Owning and inverse-side declarations must agree.
9.  Invalid or ambiguous relationship definitions must fail validation
    before generation.

### 6.2 Relationship-specific rules

  -----------------------------------------------------------------------
  Relationship                        Rules
  ----------------------------------- -----------------------------------
  `MANY_TO_ONE`                       Must identify a valid target and an
                                      owning-side join column.

  `ONE_TO_MANY`                       Normally inverse-side; must
                                      identify the owning-side property
                                      using `mappedBy`.

  `ONE_TO_ONE`                        Must identify a valid owning side
                                      and foreign-key mapping.

  `MANY_TO_MANY`                      Must define a join table and both
                                      join columns on the owning side;
                                      the inverse side must map back to
                                      the owning property.
  -----------------------------------------------------------------------

The exact JSON representation for each relationship must be finalized
against the current `RelationshipResolver` implementation before
treating this document as an executable contract.

## 7. Naming conventions

  Element                 Convention     Example
  ----------------------- -------------- ------------------------
  Database table          `snake_case`   `customer_account`
  Entity class            `PascalCase`   `CustomerAccount`
  Database column         `snake_case`   `customer_name`
  Java property           `camelCase`    `customerName`
  Relationship property   `camelCase`    `customer`
  Java package            lowercase      `com.example.entities`

Rules:

1.  Generated class and property names must be valid Java identifiers.
2.  Java reserved keywords must not be used as generated identifiers.
3.  Name conversion must be deterministic.
4.  Two different database names must not silently convert to the same
    Java property name.
5.  The generator must detect collisions between scalar fields and
    relationship properties.
6.  Entity class names must not collide with other generated types in
    the same package.

## 8. JSON formatting and syntax

Every input must be valid JSON.

1.  Use double quotes for property names and string values.
2.  Do not include comments.
3.  Do not include trailing commas.
4.  Use `true`, `false`, and `null` as JSON literals, not quoted
    strings.
5.  Use consistent indentation; four spaces are recommended.
6.  Do not duplicate object property names.
7.  Do not use inconsistent JSON value types for the same property.
8.  Reject unknown properties unless the schema version explicitly
    permits them.
9.  Avoid empty strings for optional values; omit the property or use
    `null` only where the schema explicitly allows it.

## 9. Complete example

This example defines a `Customer` entity and an `Account` entity. Each
account has one customer, represented by a `MANY_TO_ONE` relationship.

``` json
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
                },
                {
                    "name": "email",
                    "type": "VARCHAR",
                    "length": 150,
                    "nullable": false,
                    "unique": true
                }
            ],
            "relationships": []
        },
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
                    "name": "account_number",
                    "type": "VARCHAR",
                    "length": 30,
                    "nullable": false,
                    "unique": true
                },
                {
                    "name": "balance",
                    "type": "DECIMAL",
                    "precision": 15,
                    "scale": 2,
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
    ]
}
```

## 10. Pre-generation validation checklist

The validator should check all of the following before any Java source
file is written:

-   [ ] Valid JSON syntax.
-   [ ] Required root properties are present.
-   [ ] Schema version is supported.
-   [ ] Package name is valid.
-   [ ] Table names and class names are unique.
-   [ ] Each table contains at least one field.
-   [ ] All database types are supported.
-   [ ] Each table has a valid primary key.
-   [ ] Primary-key generation configuration is valid.
-   [ ] Length, precision, and scale are valid for their types.
-   [ ] Database column names are unique within each table.
-   [ ] Generated Java class and property names are valid and
    collision-free.
-   [ ] All relationship targets exist.
-   [ ] Relationship types and required properties are valid.
-   [ ] All referenced columns and relationship properties exist.
-   [ ] Owning and inverse relationship mappings are consistent.
-   [ ] No unsupported properties are present.

## 11. Project-level development principles

1.  **JSON is the source of truth:** generated entities must reflect the
    input schema.
2.  **Validate before generating:** no source files should be written if
    validation fails.
3.  **Avoid silent assumptions:** missing mandatory information must
    produce a clear validation error.
4.  **Separate responsibilities:** parsing, schema validation, data type
    mapping, relationship resolution, entity generation, and file
    writing must remain separate components.
5.  **Generate deterministically:** identical input and generator
    versions must produce identical output.
6.  **Fail fast:** report invalid configuration before writing generated
    files.
7.  **Design for extension:** new database types, relationship types,
    and annotations should be added without rewriting unrelated
    components.
8.  **Version the contract:** incompatible JSON format changes require a
    schema-version change.
9.  **Test each layer:** parser, validator, mapper, resolver, generator,
    and writer should have independent tests, plus end-to-end tests.
10. **Write safely:** generation should avoid leaving partially written
    output when an error occurs.

## 12. Implementation alignment and open decisions

This document is the proposed JSON input contract. Before declaring
version 1.0 final, verify these details against the actual code:

-   Confirm the exact relationship property names consumed by the parser
    and `RelationshipResolver`.
-   Confirm whether omitted `relationships` is accepted or whether an
    empty array is mandatory.
-   Confirm the supported database type aliases in `DataTypeMapper`.
-   Confirm whether composite primary keys are intentionally
    unsupported.
-   Confirm the intended defaults for `nullable`, `generated`,
    `optional`, and `fetch`.
-   Confirm whether `JSON` and `JSONB` should map to `String` or a
    richer Java representation.

Keep this file and the implementation tests in sync. A rule is
enforceable only when the parser and validator actually check it.
