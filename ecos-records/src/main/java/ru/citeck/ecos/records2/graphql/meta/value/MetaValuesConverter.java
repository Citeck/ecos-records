package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.factory.MetaValueFactory;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetaValuesConverter {

    private final Map<Class<?>, MetaValueFactory<Object>> valueFactories = new ConcurrentHashMap<>();

    public MetaValuesConverter(RecordsServiceFactory factory) {

        for (MetaValueFactory<?> valueFactory : factory.getMetaValueFactories()) {
            for (Class<?> type : valueFactory.getValueTypes()) {
                @SuppressWarnings("unchecked")
                MetaValueFactory<Object> objFactory = (MetaValueFactory<Object>) valueFactory;
                valueFactories.put(type, objFactory);
            }
        }
    }

    public MetaValue toMetaValue(Object value) {

        if (value == null || value instanceof RecordRef && RecordRef.isEmpty((RecordRef) value)) {
            return null;
        }
        if (value instanceof MetaValue) {
            return (MetaValue) value;
        }

        MetaValueFactory<Object> factory = valueFactories.get(value.getClass());
        if (factory == null) {
            factory = valueFactories.get(Object.class);
        }

        return factory.getValue(value);
    }
}
