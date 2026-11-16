# Code Generator V3 JSON ruleset

Use this ruleset and the architecture handoff I provide to produce input files for my Java 21 / Spring Boot Code Generator V3. The handoff defines the application's design. This ruleset defines which parts the generator can express and the exact JSON keys it accepts.

## Task and output

- Generate one set per requested service or deployable module. Label the service when returning multiple sets. Each run reads one set from `model-package-generator/schema/`.
- Output each file under its exact path in a separate `json` code block. Use `"schemaVersion": "1.0"` in each file and only the keys listed below. Use standard JSON with unique keys, double-quoted keys and strings, real JSON booleans and integers, and no comments or trailing commas.
- Use valid Java package names and unqualified Java class names. Match case exactly when referring to classes, fields, and path variables.
- Use exact names from the handoff. `None` means the feature is intentionally absent. Omit its optional key. `TBD`, conflicting details, and unconfirmed naming proposals belong outside the JSON. Apply the documented generator defaults where they fit the contract. Ask focused questions for missing details that affect the generated schemas.
- List manual work outside the JSON. If a required contract cannot be expressed, explain the gap before emitting any affected file that would change that contract. Preserve the specified routes, payloads, keys, and joins.
- The entity file is required for a generator run. Include the DTO and API files when those layers are in scope. Skip `dto-schema.json` when no DTOs are generated and no endpoint refers to a DTO. Skip `api-schema.json` when no controllers are generated. Each included file needs a non-empty top-level definition array.

## 1. `schema/schema.json` — entities and repositories

**Root keys** are `schemaVersion`, `packageName`, and `tables`. The package must end in `.entity`, for example `com.example.orders.entity`. Repositories go in the sibling `.repository` package and use the generated entity's ID type.

**Each table** has `name` (physical table name), `className` (exact Java entity class name), `fields` (non-empty array), and `relationships` (array, use `[]` when empty).

**Each field** has `name` (physical column name) and `type` (supported SQL type). Optional keys are:

| Key | Accepted value and meaning |
| --- | --- |
| `primaryKey` | Boolean. Mark every primary-key column with `true`. |
| `generated` | Boolean. `true` emits JPA `GenerationType.IDENTITY` and is valid only on a single-column primary key. Omit or use `false` for application-assigned IDs. |
| `nullable` | Boolean. Use `false` for known required columns. For a primary key, use `false` or omit this key. |
| `unique` | Boolean. Use `true` for a single column that is unique. |
| `length` | Positive integer, where applicable. Use `"type": "VARCHAR", "length": 100` for `VARCHAR(100)`. |
| `precision`, `scale` | Integers, where applicable. Precision must be positive and scale must be nonnegative and at most precision when both are set. |

Every table needs a primary key. For a composite primary key, keep the columns in declared order and mark each with `"primaryKey": true`. Leave `generated` omitted or set it to `false` for each component. V3 generates an `<Entity>Id` class and an `@EmbeddedId` property named `id`. Avoid class and field names that collide with them.

Keep physical names as defined in the handoff. For lowercase snake_case columns, the generated property converts `customer_id` to `customerId`. Check that physical names and resulting Java fields are unique in their respective scopes. Check the generated Java spelling when a schema uses mixed-case or quoted identifiers.

### Supported column types and generated Java types

| SQL type or alias | Java type |
| --- | --- |
| `VARCHAR`, `CHAR`, `TEXT`, `CLOB`, `LONGVARCHAR`, `NVARCHAR`, `INET` | `String` |
| `BIGINT` | `Long` |
| `INTEGER`, `INT` | `Integer` |
| `SMALLINT`, `TINYINT` | `Short` |
| `DECIMAL`, `NUMERIC`, `NUMBER` | `BigDecimal` |
| `FLOAT`, `DOUBLE` | `Double` |
| `REAL` | `Float` |
| `BOOLEAN`, `BOOL`, `BIT` | `Boolean` |
| `DATE` | `LocalDate` |
| `TIME` | `LocalTime` |
| `TIME WITH TIME ZONE` | `OffsetTime` |
| `TIMESTAMP`, `DATETIME`, `TIMESTAMP WITHOUT TIME ZONE` | `LocalDateTime` |
| `TIMESTAMP WITH TIME ZONE`, `TIMESTAMPTZ` | `OffsetDateTime` |
| `UUID` | `UUID` |
| `BINARY`, `VARBINARY`, `BLOB`, `LONGBLOB` | `byte[]` |
| `JSON`, `JSONB` | `String` |
| `TEXT[]` | `String[]` in generated entities |

Use uppercase type names. Flag SQL defaults, database schemas, indexes, check constraints, composite uniqueness, unsupported types, and ID generation strategies other than identity as manual work when the handoff specifies them.

### Relationships

Declare a relationship once in the owning table's `relationships` array. Every entry has `type`, `field` (owning Java property name), and `target` (exact target entity `className` in this schema).

- **`MANY_TO_ONE` or `ONE_TO_ONE`** requires `joinColumn`, the owning table's physical FK column, and `referencedColumn`, the target table's physical referenced column. Define both columns in their respective tables' `fields` arrays and use compatible types. `optional` is a boolean that defaults to `true` and controls join nullability. Set it to `false` for a required FK. Match the column's documented nullability.
- **`MANY_TO_MANY`** requires `joinTable`, `joinColumn` (join-table FK to the owner), and `inverseJoinColumn` (join-table FK to the target). The generated mapping references a single-column primary key on each side. Model a join table with its own payload columns or entity identity as a table with its own owning relationships.
- **`inverseField`** optionally names a generated reverse Java property on the target. A `MANY_TO_ONE` creates a `ONE_TO_MANY` inverse this way. Use only the supported owning types in JSON and leave `mappedBy` to the generator. Ensure the owning and inverse property names avoid collisions with scalar fields and other relationships.
- **`fetch`** optionally takes `LAZY` or `EAGER`. It defaults to `LAZY`.
- **`mapsId`** is supported on owning `MANY_TO_ONE` and `ONE_TO_ONE` mappings where the source has a composite primary key and the target has a single-column primary key. Its value is the Java name of the source ID component, such as `customerId`. That component's physical column must equal `joinColumn`. `referencedColumn` must be the target PK. Use at most one relationship per ID component.
- Keep cross-service logical IDs as scalar columns when their target is owned by another service. A multi-column foreign key, single-column shared primary-key association, or custom join predicate requires manual mapping.

## 2. `schema/dto-schema.json` — request and response DTOs

**Root keys** are `schemaVersion`, `packageName`, and `dtos`. Use a DTO base package, for example `com.example.orders.dto`. The generator adds `.request` or `.response` based on the DTO direction.

**Each DTO** has `name` (complete class name, unique across the DTO schema), `sourceEntity` (exact entity `className` in `schema.json`), `type` (`REQUEST` or `RESPONSE`), and `fields` (non-empty array).

**Each DTO field** has `name` (generated Java field name) and exactly one of the following keys:

- `sourceField` references the Java name derived from a column in the source entity's field list. The SQL type determines its Java type. For example, a column called `total_amount` is referenced as `"sourceField": "totalAmount"`.
- `sourceRelationship` references the exact `field` of an owning relationship in that entity's `relationships` array. A to-one relationship produces the target ID type, including the generated `<Target>Id` type for a composite target key. A `MANY_TO_MANY` relationship produces `List<TargetIdType>`. Use a scalar `sourceField` when the contract exposes the FK value for a non-PK join.

Preserve the handoff's selected fields and field order. Use valid, unique Java field names without accessor collisions. DTOs generated by this schema infer all types from one source entity. Flag custom types, nested objects, calculated or cross-entity fields, bean validation, JSON aliases, and `TEXT[]` DTO fields for manual work. A response DTO describes the payload inside `data`.

## 3. `schema/api-schema.json` — controllers and services

**Root keys** are `schemaVersion`, `packageName`, and `controllers`. Use the API base package for the target service, for example `com.example.orders`. V3 adds `.controller`, `.service`, and `.api` to that base.

**Each controller** has `className`, `basePath`, and `endpoints` (non-empty array). Use `""` or `"/"` for a root base path. Other base paths begin with `/` and omit the trailing slash.

| Endpoint key | Required | Accepted value and meaning |
| --- | --- | --- |
| `methodName` | Yes | Java method name used in the controller and service. Avoid names inherited from `Object`, such as `getClass`, `wait`, and `toString`. |
| `httpMethod` | Yes | `GET`, `POST`, `PUT`, `PATCH`, or `DELETE`, spelled exactly as shown. |
| `path` | Yes | Path relative to `basePath`. Use `""` for the base path itself or a slash-prefixed path like `"/{orderId}"`. |
| `serviceClass` | Yes | Exact concrete service class name. The generator injects each referenced service through the controller's constructor. |
| `pathVariables` | No | Ordered array of objects with exactly `name` and `javaType`. Use the placeholder name without braces. |
| `requestDto` | No | Exact simple name of a `REQUEST` DTO. Adds a single `@RequestBody` parameter named `request` after the path variables. |
| `responseDto` | No | Exact simple name of a `RESPONSE` DTO. Omit when the success response uses an envelope with `data: null`. |
| `successStatus` | No | Integer from 200 through 299, excluding 204 and 205. Defaults to 200. |
| `successMessage` | No | Non-blank string. Defaults to `"Success"`. |

- Paths support literal segments using ASCII letters, digits, dots, underscores, tildes, and hyphens, plus whole-segment placeholders such as `{orderId}`. Bind every placeholder from the combined route exactly once. Omit unused path variables. Wildcards, regex segments, and query strings require other handling.
- Supported `javaType` values for path variables are `String`, `UUID`, `BigDecimal`, `BigInteger`, `Long`, `Integer`, `Short`, `Byte`, `Double`, `Float`, `Boolean`, `Character`, `long`, `int`, `short`, `byte`, `double`, `float`, `boolean`, and `char`. Use the exact simple name. Avoid the reserved names `ApiResponse`, `ResponseEntity`, and `HttpStatus` for variables, and avoid `request` when the endpoint has a request DTO.
- Keep full HTTP method and route combinations unique across controllers. Treat `/orders/{id}` and `/orders/{orderId}` as the same route shape. Controller method signatures must be unique within a controller. Two endpoints that use the same service, method name, and ordered argument types must agree on the response DTO or its absence. Keep generated class names and default Spring bean names distinct, including `ApiExceptionHandler`.
- Success uses `ResponseEntity<ApiResponse<T>>` with `success`, `message`, and `data`. The shared advice formats Spring MVC failures with the same envelope. Generated service methods raise HTTP 501 until implemented.
- Flag query and header bindings, multipart bodies, top-level lists or pages, file or streaming responses, bodyless 204/205 responses, custom error bodies, and business logic for manual work when required. These are outside the accepted JSON fields.

## Matching example

Use these examples to understand the shape and references. Take all application values from the handoff.

### `schema/schema.json`

```json
{
  "schemaVersion": "1.0",
  "packageName": "com.example.orders.entity",
  "tables": [
    {
      "name": "customers",
      "className": "Customer",
      "fields": [
        {"name": "customer_id", "type": "UUID", "primaryKey": true, "nullable": false}
      ],
      "relationships": []
    },
    {
      "name": "orders",
      "className": "Order",
      "fields": [
        {"name": "order_id", "type": "BIGINT", "primaryKey": true, "generated": true, "nullable": false},
        {"name": "customer_id", "type": "UUID", "nullable": false}
      ],
      "relationships": [
        {
          "type": "MANY_TO_ONE",
          "field": "customer",
          "target": "Customer",
          "joinColumn": "customer_id",
          "referencedColumn": "customer_id",
          "inverseField": "orders",
          "optional": false
        }
      ]
    }
  ]
}
```

### `schema/dto-schema.json`

```json
{
  "schemaVersion": "1.0",
  "packageName": "com.example.orders.dto",
  "dtos": [
    {
      "name": "CreateOrderRequest",
      "sourceEntity": "Order",
      "type": "REQUEST",
      "fields": [
        {"name": "customerId", "sourceRelationship": "customer"}
      ]
    },
    {
      "name": "OrderResponse",
      "sourceEntity": "Order",
      "type": "RESPONSE",
      "fields": [
        {"name": "orderId", "sourceField": "orderId"},
        {"name": "customerId", "sourceRelationship": "customer"}
      ]
    }
  ]
}
```

### `schema/api-schema.json`

```json
{
  "schemaVersion": "1.0",
  "packageName": "com.example.orders",
  "controllers": [
    {
      "className": "OrderController",
      "basePath": "/api/orders",
      "endpoints": [
        {
          "methodName": "createOrder",
          "httpMethod": "POST",
          "path": "",
          "serviceClass": "OrderService",
          "requestDto": "CreateOrderRequest",
          "responseDto": "OrderResponse",
          "successStatus": 201,
          "successMessage": "Order created"
        },
        {
          "methodName": "getOrder",
          "httpMethod": "GET",
          "path": "/{orderId}",
          "serviceClass": "OrderService",
          "pathVariables": [{"name": "orderId", "javaType": "Long"}],
          "responseDto": "OrderResponse"
        }
      ]
    }
  ]
}
```

## Check before returning the files

1. Check every join column against its own table and each target against the target entity's exact name and referenced columns.
2. Check every DTO source against the entity schema. Check endpoint DTO names, directions, and inferred field types against the handoff.
3. Check combined paths, typed path variables, method signatures, and service names. Verify each generated success status and payload shape against the handoff.
4. Return the JSON files and any remaining decisions or manual work.
