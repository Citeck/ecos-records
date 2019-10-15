package ru.citeck.ecos.records2.graphql.meta.value;

import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.factory.MetaValueFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetaValuesConverter {

    private Map<Class<?>, MetaValueFactory> valueFactories = new ConcurrentHashMap<>();

    public MetaValuesConverter(RecordsServiceFactory factory) {

        for (MetaValueFactory<?> valueFactory : factory.getMetaValueFactories()) {
            for (Class<?> type : valueFactory.getValueTypes()) {
                valueFactories.put(type, valueFactory);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public MetaValue toMetaValue(Object value, QueryContext context, MetaField field) {

        MetaValueFactory<Object> factory = valueFactories.get(value.getClass());
        if (factory == null) {
            factory = valueFactories.get(Object.class);
        }

        MetaValue metaValue = factory.getValue(value);
        metaValue.init(context, field);

        return metaValue;
    }
}
