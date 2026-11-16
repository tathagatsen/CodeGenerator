package com.codegenerator.model_package_generator.validation;

import com.codegenerator.model_package_generator.model.api.ApiSchema;
import com.codegenerator.model_package_generator.model.api.ControllerDefinition;
import com.codegenerator.model_package_generator.model.api.EndpointDefinition;
import com.codegenerator.model_package_generator.model.api.HttpMethod;
import com.codegenerator.model_package_generator.model.api.PathVariableDefinition;
import com.codegenerator.model_package_generator.resolver.ApiTypeResolver;

import javax.lang.model.SourceVersion;
import java.beans.Introspector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiSchemaValidator {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{(" + IDENTIFIER + ")\\}");
    private static final Pattern PATH = Pattern.compile("(?:/(?:[A-Za-z0-9._~-]+|" + PATH_VARIABLE + "))*/?");
    private static final Set<String> RESTRICTED_TYPE_NAMES = Set.of("var", "yield", "record", "sealed", "permits");
    private static final Set<String> OBJECT_METHODS = Set.of(
            "clone", "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait"
    );
    private static final Set<String> RESERVED_PARAMETER_NAMES = Set.of("ApiResponse", "ResponseEntity", "HttpStatus");

    public void validate(ApiSchema schema, ApiTypeResolver types) {
        require(schema != null, "API schema must be a JSON object.");
        require("1.0".equals(schema.schemaVersion()), "API schemaVersion must be '1.0'.");
        require(schema.packageName() != null && SourceVersion.isName(schema.packageName(), SourceVersion.RELEASE_21),
                "Invalid API packageName '" + schema.packageName() + "'.");
        require(schema.controllers() != null && !schema.controllers().isEmpty(),
                "API schema must define at least one controller.");

        Set<String> controllerNames = new HashSet<>();
        Set<Route> routes = new HashSet<>();
        Map<String, Map<EndpointDefinition.Signature, EndpointDefinition>> serviceMethods = new HashMap<>();
        Map<String, String> beanTypes = new HashMap<>();
        registerBean(beanTypes, schema.responsePackage(), "ApiExceptionHandler");

        for (ControllerDefinition controller : schema.controllers()) {
            require(controller != null && isClassName(controller.className()), "Invalid API controller className.");
            String controllerName = controller.className();
            require(controllerNames.add(controllerName), "Duplicate API controller '" + controllerName + "'.");
            registerBean(beanTypes, schema.controllerPackage(), controllerName);
            require(isPath(controller.basePath()) && validBasePath(controller.basePath()),
                    controllerName + " has invalid basePath '" + controller.basePath() + "'.");
            require(controller.endpoints() != null && !controller.endpoints().isEmpty(),
                    controllerName + " must define at least one endpoint.");

            Set<EndpointDefinition.Signature> controllerMethods = new HashSet<>();
            for (EndpointDefinition endpoint : controller.endpoints()) {
                Route route = validateEndpoint(controller, endpoint, types);
                require(routes.add(route), "Duplicate API route " + endpoint.httpMethod() + " "
                        + combinedPath(controller.basePath(), endpoint.path()) + ".");
                require(controllerMethods.add(endpoint.signature()),
                        controllerName + " has a duplicate signature for method '" + endpoint.methodName() + "'.");
                registerBean(beanTypes, schema.servicePackage(), endpoint.serviceClass());
                registerServiceMethod(serviceMethods, endpoint);
            }
        }
    }

    private Route validateEndpoint(ControllerDefinition controller, EndpointDefinition endpoint, ApiTypeResolver types) {
        require(endpoint != null, controller.className() + " contains a null endpoint.");
        String context = controller.className() + "." + endpoint.methodName();
        require(isIdentifier(endpoint.methodName()) && !OBJECT_METHODS.contains(endpoint.methodName()),
                context + " has an invalid endpoint methodName.");
        require(endpoint.httpMethod() != null, context + " must specify httpMethod.");
        require(isClassName(endpoint.serviceClass()), context + " has invalid serviceClass '"
                + endpoint.serviceClass() + "'.");
        require(isPath(endpoint.path()), context + " has invalid path '" + endpoint.path() + "'.");
        require(endpoint.successStatus() >= 200 && endpoint.successStatus() <= 299
                        && endpoint.successStatus() != 204 && endpoint.successStatus() != 205,
                context + " must use a 2xx successStatus that supports a response body.");
        require(!endpoint.successMessage().isBlank(), context + " has a blank successMessage.");

        String path = combinedPath(controller.basePath(), endpoint.path());
        try {
            validatePathVariables(endpoint, path, types);
            if (endpoint.requestDto() != null) {
                types.dtoType(endpoint.requestDto(), "REQUEST");
            }
            if (endpoint.responseDto() != null) {
                types.dtoType(endpoint.responseDto(), "RESPONSE");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(context + ": " + exception.getMessage(), exception);
        }

        return new Route(endpoint.httpMethod(), PATH_VARIABLE.matcher(path).replaceAll("{}"));
    }

    private void validatePathVariables(EndpointDefinition endpoint, String path, ApiTypeResolver types) {
        Set<String> placeholders = new HashSet<>();
        Matcher matcher = PATH_VARIABLE.matcher(path);
        while (matcher.find()) {
            require(placeholders.add(matcher.group(1)), "Repeated path placeholder '" + matcher.group(1) + "'.");
        }

        Set<String> parameterNames = new HashSet<>();
        for (PathVariableDefinition variable : endpoint.pathVariables()) {
            require(variable != null && isIdentifier(variable.name()), "Invalid path variable name.");
            require(!RESERVED_PARAMETER_NAMES.contains(variable.name()),
                    "Path variable name '" + variable.name() + "' is reserved by the generated code.");
            require(parameterNames.add(variable.name()), "Duplicate path variable '" + variable.name() + "'.");
            types.pathVariableType(variable.javaType());
        }
        require(endpoint.requestDto() == null || !parameterNames.contains("request"),
                "Path variable 'request' conflicts with the request body argument.");
        require(placeholders.equals(parameterNames), "pathVariables must match the placeholders in '" + path + "'.");
    }

    private void registerServiceMethod(Map<String, Map<EndpointDefinition.Signature, EndpointDefinition>> methods,
                                       EndpointDefinition endpoint) {
        EndpointDefinition previous = methods.computeIfAbsent(endpoint.serviceClass(), name -> new HashMap<>())
                .putIfAbsent(endpoint.signature(), endpoint);
        require(previous == null || Objects.equals(previous.responseDto(), endpoint.responseDto()),
                endpoint.serviceClass() + "." + endpoint.methodName() + " has conflicting return DTOs.");
    }

    private void registerBean(Map<String, String> beans, String packageName, String className) {
        String beanName = Introspector.decapitalize(className);
        String type = packageName + "." + className;
        String previous = beans.putIfAbsent(beanName, type);
        require(previous == null || previous.equals(type),
                "Generated Spring bean name '" + beanName + "' is shared by " + previous + " and " + type + ".");
    }

    private String combinedPath(String basePath, String endpointPath) {
        if (basePath.equals("/") && !endpointPath.isEmpty()) {
            return endpointPath;
        }
        String path = basePath + endpointPath;
        return path.isEmpty() ? "/" : path;
    }

    private boolean validBasePath(String path) {
        return path.equals("/") || !path.endsWith("/");
    }

    private boolean isPath(String path) {
        return path != null && PATH.matcher(path).matches();
    }

    private boolean isClassName(String name) {
        return isIdentifier(name) && !RESTRICTED_TYPE_NAMES.contains(name);
    }

    private boolean isIdentifier(String name) {
        return name != null && IDENTIFIER.matcher(name).matches()
                && SourceVersion.isName(name, SourceVersion.RELEASE_21);
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }

    private record Route(HttpMethod httpMethod, String path) {}
}
