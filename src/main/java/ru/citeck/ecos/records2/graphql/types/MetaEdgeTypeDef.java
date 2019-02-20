package ru.citeck.ecos.records2.graphql.types;

import graphql.Scalars;
import graphql.schema.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.List;

public class MetaEdgeTypeDef implements GqlTypeDefinition {

    public static final String TYPE_NAME = "MetaEdge";

    public static GraphQLTypeReference typeRef() {
        return new GraphQLTypeReference(TYPE_NAME);
    }

    @Override
    public GraphQLObjectType getType() {

        return GraphQLObjectType.newObject()
                .name(TYPE_NAME)
                .description("Meta value edge")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .dataFetcher(this::getName)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("val")
                        .dataFetcher(this::getValue)
                        .type(MetaValueTypeDef.typeRef()))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("vals")
                        .dataFetcher(this::getValues)
                        .type(GraphQLList.list(MetaValueTypeDef.typeRef())))
                .build();
    }

    private String getName(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getName();
    }

    private Object getValue(DataFetchingEnvironment env) {
        return getValues(env).stream().findFirst().orElse(null);
    }

    private List<MetaValue> getValues(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getValues();
    }
}
