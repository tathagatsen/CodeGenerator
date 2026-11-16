package com.codegenerator.model_package_generator;

import com.codegenerator.model_package_generator.generator.ApiGenerator;
import com.codegenerator.model_package_generator.model.api.ApiSchema;
import com.codegenerator.model_package_generator.model.dto.DtoDefinition;
import com.codegenerator.model_package_generator.model.dto.DtoSchema;
import com.codegenerator.model_package_generator.parser.ApiSchemaParser;
import com.codegenerator.model_package_generator.resolver.ApiTypeResolver;
import com.codegenerator.model_package_generator.validation.ApiSchemaValidator;
import com.squareup.javapoet.JavaFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiSchemaGenerationTest {

    private static final String API_JSON = """
            {
              "schemaVersion": "1.0",
              "packageName": "example.customer",
              "controllers": [
                {
                  "className": "CustomerController",
                  "basePath": "/customers/{customerId}",
                  "endpoints": [
                    {
                      "methodName": "updateCustomer",
                      "httpMethod": "PUT",
                      "path": "",
                      "serviceClass": "CustomerService",
                      "pathVariables": [{"name": "customerId", "javaType": "UUID"}],
                      "requestDto": "UpdateCustomerRequest",
                      "responseDto": "CustomerResponse",
                      "successMessage": "Customer updated"
                    },
                    {
                      "methodName": "archiveCustomer",
                      "httpMethod": "DELETE",
                      "path": "",
                      "serviceClass": "ArchiveService",
                      "pathVariables": [{"name": "customerId", "javaType": "UUID"}]
                    }
                  ]
                },
                {
                  "className": "LegacyCustomerController",
                  "basePath": "/legacy/customers",
                  "endpoints": [
                    {
                      "methodName": "updateCustomer",
                      "httpMethod": "PUT",
                      "path": "/{id}",
                      "serviceClass": "CustomerService",
                      "pathVariables": [{"name": "id", "javaType": "UUID"}],
                      "requestDto": "UpdateCustomerRequest",
                      "responseDto": "CustomerResponse"
                    }
                  ]
                }
              ]
            }
            """;

    private final ApiSchemaValidator validator = new ApiSchemaValidator();
    private final ApiTypeResolver types = new ApiTypeResolver(new DtoSchema("1.0", "example.payload", List.of(
            new DtoDefinition("UpdateCustomerRequest", "Customer", "REQUEST", List.of()),
            new DtoDefinition("CustomerResponse", "Customer", "RESPONSE", List.of())
    )));

    @TempDir
    Path directory;

    @Test
    void generatesControllersServicesAndSharedResponses() throws IOException {
        ApiSchema schema = parse(API_JSON);
        validator.validate(schema, types);

        Map<String, JavaFile> files = new ApiGenerator(types).generate(schema);

        assertThat(files).containsOnlyKeys(
                "example.customer.controller.CustomerController",
                "example.customer.controller.LegacyCustomerController",
                "example.customer.service.CustomerService",
                "example.customer.service.ArchiveService",
                "example.customer.api.ApiExceptionHandler",
                "example.customer.api.ApiResponse"
        );
        assertThat(files.get("example.customer.controller.CustomerController").toString())
                .contains("CustomerController(CustomerService customerService, ArchiveService archiveService)",
                        "import example.payload.request.UpdateCustomerRequest;",
                        "ResponseEntity<ApiResponse<CustomerResponse>>",
                        "@PathVariable(\"customerId\") UUID customerId",
                        "@RequestBody UpdateCustomerRequest request",
                        "this.customerService.updateCustomer(customerId, request)",
                        "ResponseEntity<ApiResponse<Void>>",
                        "this.archiveService.archiveCustomer(customerId);");
        JavaFile service = files.get("example.customer.service.CustomerService");
        assertThat(service.typeSpec.methodSpecs).hasSize(1);
        assertThat(service.toString()).contains("throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED)");
        assertThat(files.get("example.customer.api.ApiExceptionHandler").toString())
                .contains("@RestControllerAdvice", "ApiResponse.failure(message)");
    }

    @Test
    void rejectsEqualRoutesEvenWhenPlaceholderNamesDiffer() throws IOException {
        ApiSchema schema = parse(API_JSON.replace("/legacy/customers", "/customers"));

        assertThatThrownBy(() -> validator.validate(schema, types))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate API route PUT /customers/{id}");
    }

    @Test
    void rejectsMissingPathVariableBinding() throws IOException {
        ApiSchema schema = parse(API_JSON.replace("{customerId}", "{id}"));

        assertThatThrownBy(() -> validator.validate(schema, types))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerController.updateCustomer", "pathVariables must match");
    }

    @Test
    void rejectsWrongDtoDirection() throws IOException {
        ApiSchema schema = parse(API_JSON.replace("\"responseDto\": \"CustomerResponse\"",
                "\"responseDto\": \"UpdateCustomerRequest\""));

        assertThatThrownBy(() -> validator.validate(schema, types))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DTO 'UpdateCustomerRequest' must have type RESPONSE");
    }

    @Test
    void rejectsNumericHttpMethod() {
        assertThatThrownBy(() -> parse(API_JSON.replace("\"httpMethod\": \"PUT\"", "\"httpMethod\": 1")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("httpMethod");
    }

    private ApiSchema parse(String json) throws IOException {
        Path file = Files.writeString(directory.resolve("api-schema.json"), json);
        return new ApiSchemaParser().parse(file);
    }
}
