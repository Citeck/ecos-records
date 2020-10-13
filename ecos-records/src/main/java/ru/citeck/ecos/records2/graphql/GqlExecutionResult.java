package ru.citeck.ecos.records2.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GraphQL execution result.
 *
 * @author Pavel Simonov
 */
public class GqlExecutionResult implements ExecutionResult {

    private ExecutionResult rawResult;
    private List<GraphQLError> errors;

    public GqlExecutionResult(ExecutionResult rawResult) {

        this.rawResult = rawResult;

        List<GraphQLError> errors = rawResult.getErrors();
        if (errors == null) {
            errors = Collections.emptyList();
        }

        this.errors = errors.stream().map(err -> {
            if (err instanceof ExceptionWhileDataFetching) {
                return expandInvocationTargetException((ExceptionWhileDataFetching) err);
            } else {
                return err;
            }
        }).collect(Collectors.toList());
    }

    private ExceptionWhileDataFetching expandInvocationTargetException(ExceptionWhileDataFetching err) {

        Throwable fetchException = err.getException();

        if (fetchException instanceof RuntimeException) {

            RuntimeException runtimeException = (RuntimeException) fetchException;
            Throwable runtimeCause = runtimeException.getCause();

            if (runtimeCause instanceof InvocationTargetException) {

                InvocationTargetException invokeExc = (InvocationTargetException) runtimeCause;

                ExecutionPath executionPath = ExecutionPath.fromList(err.getPath());
                SourceLocation location = err.getLocations()
                                             .stream()
                                             .findFirst()
                                             .orElse(null);

                return new ExceptionWhileDataFetching(executionPath, invokeExc.getTargetException(), location);

            } else {
                return err;
            }
        } else {
            return err;
        }
    }

    @Override
    public <T> T getData() {
        return rawResult.getData();
    }

    @Override
    public List<GraphQLError> getErrors() {
        return errors;
    }

    @Override
    public Map<Object, Object> getExtensions() {
        return rawResult.getExtensions();
    }

    @Override
    public Map<String, Object> toSpecification() {
        Map<String, Object> result = new LinkedHashMap<>();
        Object data = getData();
        if (data != null) {
            result.put("data", data);
        }
        if (errors != null && !errors.isEmpty()) {
            result.put("errors", errorsToSpec(errors));
        }
        Map<Object, Object> extensions = rawResult.getExtensions();
        if (extensions != null) {
            result.put("extensions", extensions);
        }
        return result;
    }

    private Object errorsToSpec(List<GraphQLError> errors) {
        return errors.stream().map(GraphQLError::toSpecification).collect(Collectors.toList());
    }
}
