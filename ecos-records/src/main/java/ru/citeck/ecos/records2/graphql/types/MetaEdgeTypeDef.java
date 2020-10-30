package ru.citeck.ecos.records2.graphql.types;

import graphql.Scalars;
import graphql.schema.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.field.MetaFieldImpl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetaEdgeTypeDef implements GqlTypeDefinition {

    public static final String TYPE_NAME = "MetaEdge";
    public static final Set<String> META_VAL_FIELDS = new HashSet<>(Arrays.asList(
        "val",
        "vals",
        "options",
        "distinct",
        "createVariants"
    ));

    private final MetaValueTypeDef metaValueTypeDef;

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
                        .name("canBeRead")
                        .dataFetcher(this::canBeRead)
                        .type(Scalars.GraphQLBoolean))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("multiple")
                        .dataFetcher(this::isMultiple)
                        .type(Scalars.GraphQLBoolean))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("options")
                        .dataFetcher(this::getOptions)
                        .type(GraphQLList.list(MetaValueTypeDef.typeRef())))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("javaClass")
                        .dataFetcher(this::getJavaClass)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("editorKey")
                        .dataFetcher(this::getEditorKey)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("type")
                        .dataFetcher(this::getEdgeType)
                        .type(Scalars.GraphQLString))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distinct")
                        .dataFetcher(this::getDistinct)
                        .type(GraphQLList.list(MetaValueTypeDef.typeRef())))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("createVariants")
                        .dataFetcher(this::getCreateVariants)
                        .type(GraphQLList.list(MetaValueTypeDef.typeRef())))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("isAssoc")
                        .dataFetcher(this::isAssociation)
                        .type(Scalars.GraphQLBoolean))
                .build();
    }

    private List<MetaValue> getCreateVariants(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return metaValueTypeDef.getAsMetaValues(edge.getCreateVariants(),
                                                env.getContext(),
                                                new MetaFieldImpl(env.getField()));
    }

    private boolean isAssociation(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.isAssociation();
    }

    private String getEdgeType(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getType();
    }

    private String getEditorKey(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.getEditorKey();
    }

    private List<MetaValue> getDistinct(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return metaValueTypeDef.getAsMetaValues(edge.getDistinct(),
                                                env.getContext(),
                                                new MetaFieldImpl(env.getField()));
    }

    private List<MetaValue> getOptions(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return metaValueTypeDef.getAsMetaValues(edge.getOptions(),
                                                env.getContext(),
                                                new MetaFieldImpl(env.getField()));
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
        MetaField field = new MetaFieldImpl(env.getField());
        return metaValueTypeDef.getAsMetaValues(edge.getValue(field), env.getContext(), field);
    }

    private boolean isMultiple(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.isMultiple();
    }

    private String getJavaClass(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        Class<?> javaClass = edge.getJavaClass();
        return javaClass != null ? javaClass.getName() : null;
    }

    private boolean isProtected(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.isProtected();
    }

    private boolean canBeRead(DataFetchingEnvironment env) {
        MetaEdge edge = env.getSource();
        return edge.isReadable();
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
