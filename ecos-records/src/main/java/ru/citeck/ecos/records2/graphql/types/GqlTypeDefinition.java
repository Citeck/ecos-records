package ru.citeck.ecos.records2.graphql.types;

import graphql.schema.GraphQLObjectType;

public interface GqlTypeDefinition {

    GraphQLObjectType getType();
}
