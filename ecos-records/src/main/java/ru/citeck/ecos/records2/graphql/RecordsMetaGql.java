package ru.citeck.ecos.records2.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.*;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.language.SourceLocation;
import graphql.parser.Parser;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.exception.GqlParseException;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records2.graphql.meta.value.field.MetaFieldImpl;
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef;
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RecordsMetaGql {

    private static final String META_QUERY_TEMPLATE = "{" + GqlMetaQueryDef.META_FIELD + "{%s}}";

    private GraphQL graphQL;

    private ObjectMapper objectMapper = new ObjectMapper();

    public RecordsMetaGql(RecordsServiceFactory serviceFactory) {

        List<GqlTypeDefinition> graphQLTypes = serviceFactory.getGqlTypes();

        Map<String, GraphQLObjectType.Builder> types = new HashMap<>();
        graphQLTypes.forEach(def -> {

            GraphQLObjectType type = def.getType();
            if (type == null) {
                log.warn("Type definition return nothing: " + def.getClass());
                return;
            }
            GraphQLObjectType.Builder builder = types.get(type.getName());

            if (builder == null) {
                builder = GraphQLObjectType.newObject(type);
                types.put(type.getName(), builder);
            } else {
                builder.fields(type.getFieldDefinitions());
            }
        });

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
        types.values().forEach(t -> {
            GraphQLObjectType type = t.build();
            if (type.getName().equals(GqlMetaQueryDef.TYPE_NAME)) {
                schemaBuilder.query(type);
            } else {
                schemaBuilder.additionalType(type);
            }
        });
        GraphQLSchema graphQLSchema = schemaBuilder.build();

        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    public List<RecordMeta> getMeta(List<?> metaValues, String schema) {

        String query = String.format(META_QUERY_TEMPLATE, schema);

        QueryContext context = QueryContext.getCurrent();
        context.setMetaValues(metaValues);

        ExecutionResult result = executeImpl(query, context);

        return convertMeta(result, metaValues);
    }

    public MetaField getMetaFieldFromSchema(String schema) {

        if (schema == null || schema.isEmpty()) {
            return EmptyMetaField.INSTANCE;
        }

        String query = String.format(META_QUERY_TEMPLATE, schema);

        Parser parser = new Parser();
        Document document = parser.parseDocument(query);

        Field field;

        try {
            field = (Field) ((SelectionSet) document.getDefinitions()
                                                    .get(0)
                                                    .getChildren()
                                                    .get(0)).getSelections().get(0);
        } catch (Exception e) {
            throw new GqlParseException("Meta field can't be received from schema", schema, e);
        }

        return new MetaFieldImpl(field);
    }

    private List<RecordMeta> convertMeta(ExecutionResult executionResult,
                                         List<?> metaValues) {

        List<RecordMeta> result = new ArrayList<>();

        if (executionResult == null || executionResult.getData() == null) {

            result = metaValues.stream()
                               .map(v -> Optional.ofNullable(RecordsUtils.getMetaValueId(v)))
                               .map(v -> new RecordMeta(v.orElse(UUID.randomUUID().toString())))
                               .collect(Collectors.toList());

        } else {

            JsonNode jsonNode = objectMapper.valueToTree(executionResult.getData());
            JsonNode meta = jsonNode.get(GqlMetaQueryDef.META_FIELD);

            for (int i = 0; i < meta.size(); i++) {
                RecordMeta recMeta = new RecordMeta(RecordsUtils.getMetaValueId(metaValues.get(i)));
                JsonNode attributes = meta.get(i);
                if (attributes instanceof ObjectNode) {
                    recMeta.setAttributes((ObjectNode) attributes);
                }
                result.add(recMeta);
            }
        }
        return result;
    }

    private ExecutionResult executeImpl(String query, Object context) {

        ExecutionInput input = ExecutionInput.newExecutionInput()
                                             .context(context)
                                             .query(query)
                                             .variables(Collections.emptyMap())
                                             .build();

        ExecutionResult result = graphQL.execute(input);
        result = new GqlExecutionResult(result);

        List<GraphQLError> errors = result.getErrors();

        if (errors != null && !errors.isEmpty()) {

            log.error("GraphQL query completed with errors:\nQuery: " + query);

            for (GraphQLError error : errors) {

                List<SourceLocation> locations = error.getLocations();
                String locationsMsg = "";
                if (locations != null && locations.size() > 0) {
                    locationsMsg = " at " + locations.stream()
                            .map(l -> l.getLine() + ":" + l.getColumn())
                            .collect(Collectors.joining(", ")) + " ";
                }

                String message = "GraphQL " + error.getErrorType() + locationsMsg + "message: " + error.getMessage();

                if (error instanceof ExceptionWhileDataFetching) {
                    log.error(message, ((ExceptionWhileDataFetching) error).getException());
                } else {
                    log.error(message);
                }
            }
        }

        return result;
    }
}
