# Code Generator V3

V3 adds Spring MVC controller and service scaffolding to the entity, repository, and DTO generator.

## Generate the API layer

1. Copy [`schema/api-schema.example.json`](model-package-generator/schema/api-schema.example.json) to `schema/api-schema.json` inside `model-package-generator`.
2. Set the package, controllers, endpoints, and service names. Match the DTO names to definitions in `schema/dto-schema.json`. The example uses the NexBank customer DTOs.
3. Run `com.codegenerator.model_package_generator.Main` with `model-package-generator` as the working directory. The generator writes to `generated/`. With `api-schema.json` absent, the same command runs V2 generation.
4. Copy the generated sources into your project's `src/main/java` and implement the service methods there. The API layer needs the Spring Boot Web MVC starter and a component scan that includes the generated base package. Each generator run can overwrite files in `generated/`.

## Define controllers and endpoints

Set `schemaVersion` to `"1.0"` and `packageName` to the base package of the target service. Each entry in `controllers` supplies a `className`, a `basePath`, and an `endpoints` array. Use `/` or `""` for a root `basePath`. Other base paths omit the trailing slash.

| Endpoint field | Meaning |
| --- | --- |
| `methodName` | Java method name used in both the controller and the service. |
| `httpMethod` | `GET`, `POST`, `PUT`, `PATCH`, or `DELETE`. |
| `path` | Route relative to `basePath`. Use `""` for the base route itself. Paths support literal segments and `{variableName}` segments. |
| `serviceClass` | Concrete `@Service` class to generate and inject. Select it per endpoint to use multiple services in a controller or share a service across controllers. |
| `pathVariables` | Optional ordered array of `{ "name": "id", "javaType": "UUID" }` entries. Bind every placeholder from the base and endpoint paths. Supported types include `String`, `UUID`, Java primitives and their boxed types, `BigDecimal`, and `BigInteger`. |
| `requestDto` | Optional name of a `REQUEST` DTO. When supplied, the controller gets an argument named `request` with `@RequestBody`. |
| `responseDto` | Optional name of a `RESPONSE` DTO. Omit for an `ApiResponse<Void>` with `data` set to `null` and a `void` service method. |
| `successStatus` | HTTP status for a successful service call. Defaults to `200`. Use a body-bearing 2xx status, such as `201` for creation. |
| `successMessage` | Message for a successful service call. Defaults to `"Success"`. |

The base package gets `controller`, `service`, and `api` subpackages. Controllers use constructor injection and pass the bound arguments directly to the selected service method. Endpoints that share a service method with identical parameter types and response DTO share the same stub.

## Response format

Controller methods return `ResponseEntity<ApiResponse<T>>`. The `ApiResponse` class supplies the JSON shape for both success and failure.

```json
{"success": true, "message": "Customer profile loaded", "data": {"firstName": "Asha"}}
```

Until implemented, each generated service method raises a `501 Not Implemented` status. The generated `@RestControllerAdvice` formats Spring MVC failures using the same envelope and preserves their HTTP status and headers. Service implementations can raise an expected failure with `ResponseStatusException`. The advice uses the HTTP reason phrase for the message. It logs unexpected exceptions and sends a generic `500` response.

```json
{"success": false, "message": "Not Implemented", "data": null}
```
