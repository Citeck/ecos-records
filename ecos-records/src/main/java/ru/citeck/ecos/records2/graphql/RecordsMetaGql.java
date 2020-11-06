package ru.citeck.ecos.records2.graphql;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.exception.GqlParseException;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField;
import ru.citeck.ecos.records2.graphql.meta.value.field.MetaFieldImpl;

@Slf4j
public class RecordsMetaGql {

    private static final String META_QUERY_TEMPLATE = "{meta{%s}}";

    public RecordsMetaGql(RecordsServiceFactory serviceFactory) {
    }

    public MetaField getMetaFieldFromSchema(String schema) {

        if (schema == null || schema.isEmpty()) {
            return EmptyMetaField.INSTANCE;
        }

        String query = String.format(META_QUERY_TEMPLATE, schema);

        Parser parser = new Parser();

        Field field;
        try {
            Document document = parser.parseDocument(query);
            field = (Field) ((SelectionSet) document.getDefinitions()
                                                    .get(0)
                                                    .getChildren()
                                                    .get(0)).getSelections().get(0);
        } catch (Exception e) {
            throw new GqlParseException("Meta field can't be received from schema", schema, e);
        }

        return new MetaFieldImpl(field);
    }
}
