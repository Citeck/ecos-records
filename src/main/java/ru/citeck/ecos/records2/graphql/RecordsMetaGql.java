package ru.citeck.ecos.records2.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.*;
import graphql.language.SourceLocation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.graphql.types.GqlMetaQueryDef;
import ru.citeck.ecos.records2.graphql.types.GqlTypeDefinition;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RecordsMetaGql {

    private static final String META_QUERY_TEMPLATE = "{" + GqlMetaQueryDef.META_FIELD + "{%s}}";

    private static final Log logger = LogFactory.getLog(RecordsMetaGql.class);

    private GraphQL graphQL;
    private Supplier<? extends GqlContext> contextSupplier;

    private ObjectMapper objectMapper = new ObjectMapper();

    public RecordsMetaGql(List<GqlTypeDefinition> graphQLTypes, Supplier<? extends GqlContext> contextSupplier) {

        this.contextSupplier = contextSupplier;

        Map<String, GraphQLObjectType.Builder> types = new HashMap<>();
        graphQLTypes.forEach(def -> {

            GraphQLObjectType type = def.getType();
            if (type == null) {
                logger.warn("Type definition return nothing: " + def.getClass());
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
        GqlContext context = contextSupplier.get();

        ExecutionResult result = executeImpl(query, context);

        return convertMeta(result, metaValues);
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

            logger.error("GraphQL query completed with errors:\nQuery: " + query);

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
                    logger.error(message, ((ExceptionWhileDataFetching) error).getException());
                } else {
                    logger.error(message);
                }
            }
        }

        return result;
    }
}
