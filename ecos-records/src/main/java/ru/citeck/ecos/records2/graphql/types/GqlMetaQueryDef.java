package ru.citeck.ecos.records2.graphql.types;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.field.MetaFieldImpl;

import java.util.List;

public class GqlMetaQueryDef implements GqlTypeDefinition {

    public static final String TYPE_NAME = "Query";
    public static final String META_FIELD = "meta";

    private MetaValueTypeDef metaValueTypeDef;

    @Override
    public GraphQLObjectType getType() {
        return GraphQLObjectType.newObject()
                .name(TYPE_NAME)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name(META_FIELD)
                        .dataFetcher(this::values)
                        .type(GraphQLList.list(MetaValueTypeDef.typeRef()))
                        .build())
                .build();
    }

    private List<MetaValue> values(DataFetchingEnvironment env) {
        QueryContext context = env.getContext();
        return metaValueTypeDef.getAsMetaValues(context.getMetaValues(),
                                                context,
                                                new MetaFieldImpl(env.getField()), true);
    }

    public void setMetaValueTypeDef(MetaValueTypeDef metaValueTypeDef) {
        this.metaValueTypeDef = metaValueTypeDef;
    }
}
