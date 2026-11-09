# CodeGenerator V2 — Updated Ruleset

**Updated:** 9 November 2026  
**Scope:** Java 21, Jakarta Persistence, Spring Data JPA, JavaPoet, Jackson  
**Coverage:** Entities, repositories, optional DTOs, composite identifiers, and Stage 1 `@MapsId`.

This document replaces the earlier V2 rules that required a DTO schema and excluded composite identifiers. It consolidates the latest agreed schema contract and demonstrated generated output. Validation and acceptance requirements describe the behavior the implementation must satisfy; they are not a claim that every check has already been tested against a repository checkout.

## Contents

1. [Inputs and generation modes](#1-inputs-and-generation-modes)
2. [Architecture and processing](#2-architecture-and-processing)
3. [Entity schema](#3-entity-schema)
4. [Naming and collisions](#4-naming-and-collisions)
5. [Data types](#5-data-types)
6. [Primary keys](#6-primary-keys)
7. [Relationship contract](#7-relationship-contract)
8. [Stage 1 MapsId](#8-stage-1-mapsid)
9. [Foreign-key column handling](#9-foreign-key-column-handling)
10. [Microservice boundaries](#10-microservice-boundaries)
11. [DTO schema](#11-dto-schema)
12. [Repositories](#12-repositories)
13. [Complete examples](#13-complete-examples)
14. [Output and diagnostics](#14-output-and-diagnostics)
15. [Validation and acceptance](#15-validation-and-acceptance)
16. [Limits and change management](#16-limits-and-change-management)

## 1. Inputs and generation modes

| Input | Required | Responsibility |
|---|---|---|
| `schema.json` | Yes | Tables, fields, keys, and owning relationships |
| `dto-schema.json` | No | Explicit request and response DTO definitions |

File locations are configured by the application. `entity-schema.json` is also a usable filename when configured; the filename itself does not change the JSON contract.

| Supplied configuration | Expected output |
|---|---|
| Entity schema only | Entities, required embeddable ID classes, repositories |
| Entity schema and valid DTO schema | All persistence artifacts plus declared DTOs |
| No DTO schema configured | Skip DTO parsing, validation, and generation |
| DTO schema explicitly configured but missing or invalid | Report the error; do not silently skip it |

Do not require an empty DTO file to enable entity-only generation. An absent optional input and a malformed supplied input are different cases.

## 2. Architecture and processing

| Component | Responsibility |
|---|---|
| `Main` | Coordinate configured inputs and the generation pipeline |
| `SchemaParser` | Parse the entity JSON |
| `SchemaValidator` | Validate tables, columns, keys, names, and relationship rules |
| `DataTypeMapper` | Resolve database types to Java types |
| `RelationshipResolver` | Resolve owning/inverse associations and derived-ID metadata |
| `EntityGenerator` | Generate entities and embeddable identifier classes |
| `RepositoryGenerator` | Generate repository interfaces with the correct ID type |
| `DtoSchemaParser` | Parse the supplied optional DTO schema |
| `DtoSchemaValidator` | Validate DTO selectors against entity metadata |
| `DtoFieldTypeResolver` | Derive scalar and relationship identifier types |
| `DtoGenerator` | Generate declared DTO classes |
| `SourceFileWriter` | Write validated generated sources |

Required dependency order:

1. Parse and validate the mandatory entity schema.
2. Resolve field types, primary-key metadata, and relationships.
3. If a DTO input is supplied, parse and validate it against the resolved model.
4. Generate entities, composite ID classes where needed, and repositories.
5. Generate DTOs only when requested.
6. Collect sources, detect duplicate output paths, and write them.

All supplied inputs must pass validation before source writing. Keep parsing, validation, resolution, generation, and writing separate.

## 3. Entity schema

### Root and tables

| Location | Property | Rule |
|---|---|---|
| Root | `schemaVersion` | Use the supported value, currently `"1.0"` in existing schemas |
| Root | `packageName` | Valid Java package for entities and embeddable IDs |
| Root | `tables` | Non-empty array |
| Table | `name` | Unique physical table name |
| Table | `className` | Unique valid Java entity class name |
| Table | `fields` | Non-empty array of column definitions |
| Table | `relationships` | Array of owning relationships; use `[]` when absent |

Do not add speculative properties such as table-level `primaryKeys`, `compositeKey`, or `embeddedId`. Primary-key membership is defined on fields.

### Field properties

| Property | Meaning |
|---|---|
| `name` | Physical database column name |
| `type` | Database type accepted by `DataTypeMapper` |
| `primaryKey` | `true` when the column participates in the primary key |
| `generated` | Whether the supported single-column key is generated |
| `nullable` | Column nullability |
| `unique` | Single-column uniqueness |
| `length` | Length for an applicable type |
| `precision` | Numeric precision for an applicable type |
| `scale` | Numeric scale for an applicable type |

Rules:

- Every table has at least one primary-key field.
- Use actual JSON booleans, not quoted boolean strings.
- Column names and converted Java names must be unique.
- Specify key and association nullability explicitly in new schemas.
- A primary-key component cannot be nullable.
- `generated: true` is invalid for non-key fields and composite-key components.
- Use positive lengths and precision; scale must be non-negative and no larger than precision.
- Omit irrelevant optional properties.
- Do not encode a multi-column unique constraint using `unique: true` on each component; that would express separate single-column constraints.

## 4. Naming and collisions

| Element | Convention | Example |
|---|---|---|
| Database table/column | `snake_case` | `user_roles`, `assigned_by` |
| Entity | `PascalCase` | `UserRole` |
| Scalar Java property | `camelCase` | `assignedBy` |
| Relationship property | Explicit Java identifier | `assignedByUser` |
| Embedded-ID class | `<Entity>Id` | `UserRoleId` |
| Embedded-ID entity property | `id` | `UserRoleId id` |
| Repository | `<Entity>Repository` | `UserRoleRepository` |

Use valid identifiers and reject Java keywords. Check names after conversion and include generated classes, the embedded `id` property, and inverse relationship fields in collision checks.

### The assigned_by conflict

A scalar column `assigned_by` converts to `assignedBy`. Naming its relationship `assignedBy` can fail validation even if scalar-FK suppression would later remove that scalar property.

Keep the physical field and use a distinct relationship name:

```json
{
  "type": "MANY_TO_ONE",
  "field": "assignedByUser",
  "target": "User",
  "joinColumn": "assigned_by",
  "referencedColumn": "user_id",
  "fetch": "LAZY"
}
```

Use descriptive `camelCase`; changing capitalization alone is not a reliable collision fix. Update DTO relationship selectors when renaming a relationship.

## 5. Data types

The installed `DataTypeMapper` is authoritative. Earlier documentation listed aliases more broadly than the demonstrated implementation; do not infer support from a database accepting a type.

| Database type | Java representation / authoring rule |
|---|---|
| `VARCHAR`, `CHAR`, `TEXT`, `CLOB`, `NVARCHAR` | `String` in the discussed mapper |
| `UUID` | `java.util.UUID` |
| `INTEGER` | `Integer` |
| `BIGINT` | `Long` |
| `BOOLEAN` | `Boolean` |
| `DECIMAL`, `NUMERIC` | `java.math.BigDecimal` |
| `DATE` | `java.time.LocalDate` |
| `TIMESTAMPTZ` | `java.time.OffsetDateTime` |
| `VARBINARY` | Binary representation supported by the mapper; use instead of unsupported `BYTEA` |
| `JSON`, `JSONB` | Basic `String` mapping in the existing contract; native JSON persistence needs separate support |
| `SMALLINT` | Earlier revisions differed between `Integer` and `Short`; check the current mapper |
| `INET`, `TEXT[]` | Used in recent schemas, but support must be checked in the current mapper |

Aliases such as `INT`, `NUMBER`, `BOOL`, `BIT`, `TINYINT`, timestamp variants, and additional binary/floating types are valid only when implemented. Do not silently substitute `String` for unsupported types. Java type mapping alone does not guarantee correct database-specific JDBC binding.

The mapping must be consistent across entity fields, ID components, repositories, and DTOs.

## 6. Primary keys

### Single-column key

Exactly one `primaryKey: true` field produces a normal `@Id` property.

For a supported generated numeric key, `generated: true` produces the implemented `@GeneratedValue(strategy = GenerationType.IDENTITY)` behavior. Do not use identity generation for UUIDs or assume a UUID strategy exists. Assigned keys must be populated by the application.

### Composite key

Two or more fields with `primaryKey: true` produce:

- A public `<Entity>Id` class in the entity package.
- `@Embeddable` and `Serializable`.
- `serialVersionUID`.
- Scalar key components with their physical `@Column` mappings.
- Getters, setters, and `equals`/`hashCode` covering every key component.
- A JPA-compatible no-argument constructor, explicit or implicit.
- An entity property `@EmbeddedId private <Entity>Id id;`.

Composite-key columns live inside the ID class. Do not also emit them as ordinary scalar properties on the entity. Non-key fields remain on the entity.

Do not generate `@GeneratedValue` on composite components. Do not use entity relationships as embeddable scalar components in Stage 1. The supported strategy is `@EmbeddedId`, not `@IdClass`.

For application code, access a component through `entity.getId().getUserId()`; a flat `entity.getUserId()` is not implied.

## 7. Relationship contract

Declare the owning side. The current supported explicit types are:

| JSON `type` | Owning mapping | Generated inverse when `inverseField` is provided |
|---|---|---|
| `MANY_TO_ONE` | `@ManyToOne` with `@JoinColumn` | Target `@OneToMany(mappedBy = "<field>")` |
| `ONE_TO_ONE` | `@OneToOne` with `@JoinColumn` | Target inverse `@OneToOne(mappedBy = "<field>")` |
| `MANY_TO_MANY` | `@ManyToMany` with `@JoinTable` | Target inverse `@ManyToMany(mappedBy = "<field>")` |

Do not explicitly declare `ONE_TO_MANY` in input JSON. It is generated from a `MANY_TO_ONE` association's `inverseField`. Do not declare the inverse again on the target table.

### Properties

| Property | Rule |
|---|---|
| `type` | One of the three explicit owning types above |
| `field` | Owning entity's Java relationship property |
| `target` | Exact target `className` in this schema |
| `joinColumn` | Owning FK column, or owner column of a many-to-many join table |
| `referencedColumn` | Physical target column for a singular join; supply explicitly in new examples |
| `joinTable` | Physical join-table name for many-to-many |
| `inverseJoinColumn` | Target FK column in a many-to-many join table |
| `inverseField` | Optional Java property generated on the target |
| `optional` | Association optionality; discussed default is `true` |
| `fetch` | `LAZY` or `EAGER`; discussed default is `LAZY` |
| `mapsId` | Embedded-ID component Java name; see Section 8 |

`inverseField` is the JSON property. `mappedBy` is generated JPA metadata, not an interchangeable JSON alias.

Rules:

- Resolve targets within the same entity schema.
- Validate physical join/referenced columns and compatible types.
- Keep association optionality consistent with column nullability.
- Enforce one-to-one uniqueness in the database design; an annotation is not a substitute for reviewing the actual constraint.
- Detect inverse-field collisions with scalar and generated properties.
- Multiple links to the same entity need distinct owner and inverse names.
- A self-reference is permitted when the target is present and names are valid.
- Do not invent cascade, orphan removal, ordering, or join-column arrays.
- A many-to-many join table with extra business columns should be modeled as an association entity with two owning relationships.

## 8. Stage 1 MapsId

`mapsId` connects a relationship to a scalar component of the owning entity's composite embedded identifier.

```json
{
  "type": "MANY_TO_ONE",
  "field": "account",
  "target": "Account",
  "joinColumn": "account_id",
  "referencedColumn": "account_id",
  "mapsId": "accountId",
  "optional": false,
  "fetch": "LAZY"
}
```

This generates `@MapsId("accountId")`. The value is the Java component name inside `AccountTransactionId`, not the database column `account_id` and not a dotted path such as `id.accountId`.

### Required checks

1. The owner has a composite primary key and an embedded ID.
2. The relationship is `MANY_TO_ONE` or `ONE_TO_ONE`.
3. The target has exactly one primary-key column.
4. `mapsId` identifies an existing embedded-ID component.
5. `joinColumn` identifies that component's physical column.
6. `referencedColumn` identifies the target primary key.
7. Component and target-key Java types agree.
8. The PK/FK mapping is non-nullable; use `optional: false`.
9. No duplicate component binding or writable column mapping is generated.

Use `mapsId` when a composite-key component is also the FK represented by the relationship. Do not infer it merely from matching column names, and do not add it to unrelated non-key associations.

### Stage 1 exclusions

- Composite-primary-key parent targets.
- Multi-column `@JoinColumns`.
- `@MapsId` on `MANY_TO_MANY`.
- Empty `@MapsId` for a shared whole identifier.
- Nested embedded-ID paths.
- Ordinary single-column shared-primary-key mappings unless separately implemented.

`ResolvedRelationship` carries the resolved `mapsIdKey`. When extending this model, update every constructor call and consumer together.

## 9. Foreign-key column handling

Column metadata stays in `fields` so validation and type resolution can inspect it. Generated entity properties follow these rules:

| Column role | Generated representation |
|---|---|
| Ordinary scalar column | Scalar property with `@Column` |
| Ordinary FK represented by an owning relationship | Relationship; suppress the duplicate scalar property |
| Composite PK component | Scalar component in `<Entity>Id` |
| Composite PK component also represented by a relationship | ID component plus relationship with `@MapsId` |

For `assigned_by` with the `assignedByUser` relationship, the normal output is the association, not an additional writable `UUID assignedBy` property.

Suppression happens during generation and does not bypass earlier naming checks. Do not use blanket `insertable = false, updatable = false` as a replacement for correct derived-ID mappings.

## 10. Microservice boundaries

Generate each service's persistence model from its own schema.

A customer, user, account, or transaction identifier owned by another service stays a scalar logical reference unless its entity genuinely belongs to the same persistence boundary and input schema.

Example:

```json
{
  "name": "customer_id",
  "type": "UUID",
  "nullable": false
}
```

Do not invent a local `Customer` entity or JPA association just because a table description mentions another service. Resolve cross-service data through service APIs or events. Only add `inverseField` for real local relationships.

## 11. DTO schema

DTO input is optional and separate from the entity schema.

### Root and definition properties

| Location | Properties |
|---|---|
| Root | `schemaVersion`, `packageName`, `dtos` |
| DTO | `name`, `sourceEntity`, `type`, `fields` |
| DTO field | `name` and exactly one of `sourceField` / `sourceRelationship` |

Rules:

- `sourceEntity` matches an entity `className`.
- `type` is exactly `REQUEST` or `RESPONSE`.
- `name` is the complete DTO class name; do not silently append suffixes.
- Request classes use `<DTO package>.request`; response classes use `<DTO package>.response`.
- Generate only explicitly selected fields.
- Selectors use Java property names, not physical database names.
- Reject both selectors, neither selector, unknown sources, duplicate names, and unsupported type overrides.
- Generate public classes, private fields, required imports, getters, and setters.
- Validate against resolved relationships, including generated inverse relationships.

### Type derivation

| Source | DTO type |
|---|---|
| `sourceField` | Java type of the selected schema scalar |
| Singular relationship to a single-key target | Target scalar ID type |
| Singular relationship to a composite-key target | `<TargetEntity>Id` |
| Collection relationship to a single-key target | `List<TargetIdType>` |
| Collection relationship to a composite-key target | `List<TargetEntityId>` |

Resolve composite ID classes with the entity package, using a correct import or fully qualified name. Do not select the first component as a substitute for a composite identifier.

A scalar selector may refer to an individual schema key component and retains its scalar type. This does not generate an entity-to-DTO mapper or flat entity accessor. Any manually written mapper must read embedded components through `entity.id`, and suppressed FK values through the association as appropriate.

Resolving a composite target ID for a DTO does not add support for composite-parent JPA joins.

## 12. Repositories

Generate one public interface per entity:

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
}
```

For a composite identifier:

```java
public interface AccountTransactionRepository
        extends JpaRepository<AccountTransaction, AccountTransactionId> {
}
```

Use `<Entity>Repository` and the configured repository package convention; the existing rules recommend `<entity package>.repository`. Import the entity, ID class when needed, and `JpaRepository`.

Do not use `Long`, `Object`, or the first key component as a fallback ID type. Do not invent finder methods, query annotations, or repository implementations.

## 13. Complete examples

### Entity schema: two parent keys and MapsId

The following is a complete entity-schema example.

```json
{
  "schemaVersion": "1.0",
  "packageName": "com.example.banking.entity",
  "tables": [
    {
      "name": "accounts",
      "className": "Account",
      "fields": [
        {
          "name": "account_id",
          "type": "BIGINT",
          "primaryKey": true,
          "generated": true,
          "nullable": false
        }
      ],
      "relationships": []
    },
    {
      "name": "transactions",
      "className": "Transaction",
      "fields": [
        {
          "name": "transaction_id",
          "type": "BIGINT",
          "primaryKey": true,
          "generated": true,
          "nullable": false
        }
      ],
      "relationships": []
    },
    {
      "name": "account_transaction",
      "className": "AccountTransaction",
      "fields": [
        {
          "name": "account_id",
          "type": "BIGINT",
          "primaryKey": true,
          "nullable": false
        },
        {
          "name": "transaction_id",
          "type": "BIGINT",
          "primaryKey": true,
          "nullable": false
        },
        {
          "name": "amount",
          "type": "DECIMAL",
          "precision": 15,
          "scale": 2,
          "nullable": false
        }
      ],
      "relationships": [
        {
          "type": "MANY_TO_ONE",
          "field": "account",
          "target": "Account",
          "joinColumn": "account_id",
          "referencedColumn": "account_id",
          "mapsId": "accountId",
          "inverseField": "accountTransactions",
          "optional": false,
          "fetch": "LAZY"
        },
        {
          "type": "MANY_TO_ONE",
          "field": "transaction",
          "target": "Transaction",
          "joinColumn": "transaction_id",
          "referencedColumn": "transaction_id",
          "mapsId": "transactionId",
          "inverseField": "accountTransactions",
          "optional": false,
          "fetch": "LAZY"
        }
      ]
    }
  ]
}
```

Expected generated artifacts include `Account`, `Transaction`, `AccountTransaction`, `AccountTransactionId`, and three repositories. Both parent entities receive an inverse `accountTransactions` collection.

Entity mapping excerpt, with imports/accessors omitted:

```java
@Entity
@Table(name = "account_transaction")
public class AccountTransaction {
    @EmbeddedId
    private AccountTransactionId id;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("accountId")
    @JoinColumn(name = "account_id",
                referencedColumnName = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("transactionId")
    @JoinColumn(name = "transaction_id",
                referencedColumnName = "transaction_id", nullable = false)
    private Transaction transaction;
}
```

The ID class contains `Long accountId` and `Long transactionId`, their column annotations, accessors, serialization metadata, and value-based equality.

### Optional DTO schema for the same model

```json
{
  "schemaVersion": "1.0",
  "packageName": "com.example.banking.dto",
  "dtos": [
    {
      "name": "AccountTransactionResponseDto",
      "sourceEntity": "AccountTransaction",
      "type": "RESPONSE",
      "fields": [
        { "name": "accountId", "sourceRelationship": "account" },
        { "name": "transactionId", "sourceRelationship": "transaction" },
        { "name": "amount", "sourceField": "amount" }
      ]
    },
    {
      "name": "AccountResponseDto",
      "sourceEntity": "Account",
      "type": "RESPONSE",
      "fields": [
        { "name": "accountId", "sourceField": "accountId" },
        {
          "name": "accountTransactionIds",
          "sourceRelationship": "accountTransactions"
        }
      ]
    }
  ]
}
```

The first DTO uses `Long`, `Long`, and `BigDecimal`. The second uses `Long` and `List<AccountTransactionId>`.

### Many-to-many owning declaration

Use this form only when both target entities are present, their keys fit the supported single-column join model, and the join table has no extra business state:

```json
{
  "type": "MANY_TO_MANY",
  "field": "roles",
  "target": "Role",
  "joinTable": "user_role_links",
  "joinColumn": "user_id",
  "inverseJoinColumn": "role_id",
  "inverseField": "users",
  "fetch": "LAZY"
}
```

For an association such as `UserRole` with `assigned_by` and `assigned_at`, use an explicit entity instead.

## 14. Output and diagnostics

- Use `jakarta.persistence` imports.
- Write one correctly named public type per Java source file.
- Match package declarations to directory paths.
- Place embeddable ID classes alongside their entities.
- Detect duplicate generated class names and output paths before writing.
- Use UTF-8 and deterministic ordering.
- Do not overwrite unrelated files silently.
- Prefer staged writes to avoid partial output.
- Report success only after writing completes.

When DTO input is absent, an appropriate informational message is:

```text
DTO schema not provided. Skipping DTO generation.
```

Errors should identify the input, table/DTO, property, and violated rule. Examples of required diagnostic detail:

```text
Table 'user_roles': relationship field 'assignedBy'
conflicts with database field 'assigned_by'.

Table 'account_transaction': mapsId 'account_id' is not an
embedded-ID Java property; expected 'accountId'.

Table 'account_transaction': mapsId target 'Account'
must have a single-column primary key.

DTO 'AccountResponseDto': sourceRelationship 'accountTransactions'
does not exist in the resolved entity model.
```

Keep actionable validation failures distinct from unexpected internal exceptions.

## 15. Validation and acceptance

The following are verification requirements, not a report of tests run.

### Schema and name validation

- [ ] Root properties, packages, table names, and class names are valid.
- [ ] Each table has fields and at least one primary-key component.
- [ ] Database types and numeric/string metadata are supported.
- [ ] Physical and converted names are unique.
- [ ] Relationship and inverse fields do not collide.
- [ ] Generated `id`, ID-class names, and repository names do not collide.
- [ ] Unknown relationship targets and unsupported properties fail clearly.

### Keys and relationships

- [ ] Single keys generate `@Id` with the correct scalar type.
- [ ] Composite keys generate a serializable embeddable ID and `@EmbeddedId`.
- [ ] Composite components are non-nullable and never generated.
- [ ] ID equality and hash code include every component.
- [ ] Stage 1 `mapsId` validates owner, component, target, columns, and types.
- [ ] Unsupported composite-parent joins and many-to-many MapsId fail.
- [ ] Generated inverse mappings point to the correct owning field.
- [ ] Ordinary FK scalar suppression does not create duplicate writable columns.
- [ ] Cross-service logical references stay scalar.

### DTOs and repositories

- [ ] Entity-only generation works without a DTO input.
- [ ] Explicitly supplied invalid DTO input fails before writes.
- [ ] DTO selectors use exactly one valid source.
- [ ] DTO scalar, singular-ID, collection-ID, and composite-ID types resolve correctly.
- [ ] Repository ID generics match scalar or embedded identifier types.
- [ ] DTOs import composite ID classes from the entity package.

### End-to-end verification

- [ ] Generate the complete example with and without DTO input.
- [ ] Compile generated source against the configured Java/JPA/Spring Data dependencies.
- [ ] Verify persistence-provider metadata for the MapsId example.
- [ ] Persist/load an association entity and verify both ID components.
- [ ] Verify equivalent ID objects behave correctly in sets/maps.
- [ ] Confirm failures leave no newly written partial source set.

## 16. Limits and change management

### Outside the current supported contract

- Composite-parent/multi-column relationship joins.
- `@IdClass`, nested embedded IDs, and additional MapsId stages.
- Explicit inverse `ONE_TO_MANY` JSON declarations.
- Nested DTO object graphs, arbitrary type overrides, and generated mappers.
- DTO validation annotations, Lombok, records, builders, and configurable constructors.
- Custom repository queries or finder generation.
- Database introspection, migration scripts, native JSON binding, and dialect profiles unless separately implemented.
- Business services, controllers, API endpoints, or automatic cross-service relationships.

Generated ID `equals`/`hashCode` and serialization support are part of the composite-key feature; general configurable entity equality generation remains outside scope.

### Updating the contract

For a contract change, update the model, parser, validator, resolver, generators, examples, ruleset, README, and meaningful tests together. Carry changes to `RelationshipDefinition`, `ResolvedRelationship`, and all constructor calls consistently.

Keep schema version `"1.0"` for the existing accepted examples unless version handling is deliberately changed in the implementation. Document and version future incompatible changes together.

### Changes consolidated in this revision

| Earlier guidance | Updated rule |
|---|---|
| DTO schema required | DTO schema optional |
| Exactly one PK column | One or more PK columns |
| Composite keys excluded | Generated `@Embeddable` + `@EmbeddedId` |
| No derived-ID support | Stage 1 `mapsId` for singular joins to single-key parents |
| Scalar-only repository ID | Scalar ID or generated `<Entity>Id` |
| Scalar-only relationship DTO ID | Full target ID, including composite ID and `List<ID>` |
| Ambiguous inverse syntax | Owning declaration plus `inverseField` |
| Explicit `ONE_TO_MANY` listed | Inverse collection generated from `MANY_TO_ONE` |
| FK suppression assumed to avoid all conflicts | Validate names first; use `assignedByUser` |
| All references treated alike | Local JPA association versus cross-service scalar reference |

---

When authoring JSON, use only implemented property names, explicitly model keys and owning relationships, select DTO fields deliberately, and validate the complete supplied input before writing generated source.

