package ru.citeck.ecos.records2.graphql.types;

import graphql.Scalars;
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.CustomGqlScalars;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.graphql.meta.value.field.MetaFieldImpl;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MetaValue GraphQL type definition.
 *
 * @author Pavel Simonov
 */
@Slf4j
@SuppressWarnings("unchecked")
public class MetaValueTypeDef implements GqlTypeDefinition {

    public static final String TYPE_NAME = "MetaValue";

    public static GraphQLTypeReference typeRef() {
        return new GraphQLTypeReference(TYPE_NAME);
    }

    private MetaValuesConverter converter;

    public MetaValueTypeDef(RecordsServiceFactory factory) {
        converter = factory.getMetaValuesConverter();
    }

    @Override
    public GraphQLObjectType getType() {

        return GraphQLObjectType.newObject()
                .name(TYPE_NAME)
                .description("Meta value")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .description("Identifier")
                        .dataFetcher(this::getId)
                        .type(Scalars.GraphQLID))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("att")
                        .description("Attribute")
                        .dataFetcher(this::getAtt)
                        .argument(GraphQLArgument.newArgument()
                                .name("n")
                                .type(Scalars.GraphQLString)
                                .build())
                        .type(typeRef()))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("atts")
                        .description("Attributes")
                        .dataFetcher(this::getAtts)
                        .argument(GraphQLArgument.newArgument()
                                .name("n")
                                .type(Scalars.GraphQLString)
                                .build())
                        .type(GraphQLList.list(typeRef())))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("edge")
                        .description("Attribute edge")
                        .dataFetcher(this::getEdge)
                        .argument(GraphQLArgument.newArgument()
                                .name("n")
                                .type(Scalars.GraphQLString)
                                .build())
                        .type(MetaEdgeTypeDef.typeRef()))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("as")
                        .description("Cast to another type")
                        .dataFetcher(this::getAs)
                        .argument(GraphQLArgument.newArgument()
                                .name("n")
                                .type(Scalars.GraphQLString)
                                .build())
                        .type(typeRef()))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("str")
                        .description("String representation")
                        .dataFetcher(this::getStr)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("assoc")
                        .description("Association")
                        .dataFetcher(this::getStr)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("disp")
                        .description("Display name")
                        .dataFetcher(this::getDisp)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("num")
                        .description("Number representation")
                        .dataFetcher(this::getNum)
                        .type(Scalars.GraphQLFloat))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bool")
                        .description("Boolean representation")
                        .dataFetcher(this::getBool)
                        .type(Scalars.GraphQLBoolean))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("has")
                        .description("Check attribute exist or not")
                        .dataFetcher(this::getHasAttribute)
                        .argument(GraphQLArgument.newArgument()
                                .name("n")
                                .type(Scalars.GraphQLString)
                                .build())
                        .type(Scalars.GraphQLBoolean))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("json")
                        .description("Json representation")
                        .dataFetcher(this::getJson)
                        .type(CustomGqlScalars.JSON_NODE))
                .build();
    }

    private boolean getHasAttribute(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        String name = getParameter(env, "n");
        try {
            return value.has(name);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to get attribute " + name, e);
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MetaValue getAs(DataFetchingEnvironment env) {

        MetaValue source = env.getSource();
        String type = getParameter(env, "n");
        Object value = source.getAs(type);

        MetaField metaField = new MetaFieldImpl(env.getField());
        return getAsMetaValue(value, env.getContext(), metaField, false);
    }

    private String getId(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        return value.getId();
    }

    private Object getAtt(DataFetchingEnvironment env) {
        return getAtts(env).stream().findFirst().orElse(null);
    }

    public List<MetaValue> getAsMetaValues(Object rawValue, QueryContext context, MetaField metaField) {
        return getAsMetaValues(rawValue, context, metaField, false);
    }

    public List<MetaValue> getAsMetaValues(Object rawValue,
                                           QueryContext context,
                                           MetaField metaField,
                                           boolean forceInit) {

        List<Object> result;

        if (rawValue == null) {

            result = Collections.emptyList();

        } else if (rawValue instanceof Collection<?>) {

            result = new ArrayList<>((Collection<?>) rawValue);

        } else if (rawValue.getClass().isArray()) {

            int length = Array.getLength(rawValue);

            if (length == 0) {

                result = Collections.emptyList();

            } else {

                result = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    result.add(Array.get(rawValue, i));
                }
            }

        } else {

            result = Collections.singletonList(rawValue);
        }

        return result.stream()
                     .map(v -> getAsMetaValue(v, context, metaField, forceInit))
                     .collect(Collectors.toList());
    }

    private MetaValue getAsMetaValue(Object value,
                                     QueryContext context,
                                     MetaField metaField,
                                     boolean forceInit) {

        if (value instanceof MetaValue) {
            MetaValue metaValue = (MetaValue) value;
            if (forceInit) {
                metaValue.init(context, metaField);
            }
            return metaValue;
        }

        MetaValue metaValue = converter.toMetaValue(value);
        metaValue.init(context, metaField);

        return metaValue;
    }

    private List<?> getAtts(DataFetchingEnvironment env) {

        MetaValue metaValue = env.getSource();
        String name = getParameter(env, "n");

        try {
            MetaField metaField = new MetaFieldImpl(env.getField());
            return getAsMetaValues(metaValue.getAttribute(name, metaField), env.getContext(), metaField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean getBool(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        return value.getBool();
    }

    private String getStr(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        return value.getString();
    }

    private String getDisp(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        return value.getDisplayName();
    }

    private Double getNum(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        return value.getDouble();
    }

    private MetaEdge getEdge(DataFetchingEnvironment env) {
        String name = env.getArgument("n");
        MetaValue value = env.getSource();
        return value.getEdge(name, new MetaFieldImpl(env.getField()));
    }

    private Object getJson(DataFetchingEnvironment env) {
        MetaValue value = env.getSource();
        return value.getJson();
    }

    private String getParameter(DataFetchingEnvironment env, String name) {
        String value = env.getArgument(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is a mandatory parameter!");
        }
        return value;
    }
}