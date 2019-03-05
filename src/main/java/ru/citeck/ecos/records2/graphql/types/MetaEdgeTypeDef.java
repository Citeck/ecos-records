package ru.citeck.ecos.records2.graphql.types;

import graphql.Scalars;
import graphql.schema.*;
import ru.citeck.ecos.records2.graphql.CustomGqlScalars;
import ru.citeck.ecos.records2.graphql.meta.value.EdgeOption;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.List;

public class MetaEdgeTypeDef implements GqlTypeDefinition {

    public static final String TYPE_NAME = "MetaEdge";

    private MetaValueTypeDef metaValueTypeDef;

    public MetaEdgeTypeDef(MetaValueTypeDef metaValueTypeDef) {
        this.metaValueTypeDef = metaValueTypeDef;
    }

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
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("title")
                        .dataFetcher(this::getTitle)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .dataFetcher(this::getDescription)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("protected")
                        .dataFetcher(this::isProtected)
                        .type(Scalars.GraphQLBoolean))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("multiple")
                        .dataFetcher(this::isMultiple)
                        .type(Scalars.GraphQLBoolean))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("options")
                        .dataFetcher(this::getOptions)
                        .type(CustomGqlScalars.JSON_NODE))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("javaClass")
                        .dataFetcher(this::getJavaClass)
                        .type(Scalars.GraphQLString))
                .build();
    }

    private String getName(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getName();
    }

    private Object getValue(DataFetchingEnvironment env) throws Exception {
        return getValues(env).stream().findFirst().orElse(null);
    }

    private List<MetaValue> getValues(DataFetchingEnvironment env) throws Exception {
        MetaEdge edge = env.getSource();
        return metaValueTypeDef.getAsMetaValues(edge.getValue(), env.getContext());
    }
    
    private boolean isMultiple(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.isMultiple();
    }
    
    private String getJavaClass(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getJavaClass().getName();
    }
    
    private List<EdgeOption> getOptions(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getOptions();
    }

    private boolean isProtected(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.isProtected();
    }

    private String getTitle(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getTitle();
    }

    private String getDescription(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getDescription();
    }
}
