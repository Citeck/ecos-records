package ru.citeck.ecos.records2.graphql.meta.value;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.factory.MetaValueFactory;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @deprecated -> AttValuesConverter
 */
@Deprecated
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

    @NotNull
    public List<MetaValue> getAsMetaValues(Object rawValue,
                                           QueryContext context,
                                           MetaField metaField,
                                           boolean forceInit) {

        List<Object> result;

        if (rawValue == null
            || rawValue instanceof RecordRef && RecordRef.isEmpty((RecordRef) rawValue)
            || rawValue instanceof DataValue && ((DataValue) rawValue).isNull()
            || rawValue instanceof JsonNode && ((JsonNode) rawValue).isNull()) {

            result = Collections.emptyList();

        } else if (rawValue instanceof HasCollectionView) {

            result = new ArrayList<>(((HasCollectionView<?>) rawValue).getCollectionView());

        } else if (rawValue instanceof Collection<?>) {

            result = new ArrayList<>((Collection<?>) rawValue);

        } else if (rawValue.getClass().isArray()) {

            if (byte[].class.equals(rawValue.getClass())) {

                result = Collections.singletonList(rawValue);

            } else {

                int length = Array.getLength(rawValue);

                if (length == 0) {

                    result = Collections.emptyList();

                } else {

                    result = new ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        result.add(Array.get(rawValue, i));
                    }
                }
            }

        } else {

            result = Collections.singletonList(rawValue);
        }

        return result.stream()
            .map(v -> getAsMetaValue(v, context, metaField, forceInit))
            .collect(Collectors.toList());
    }

    public MetaValue getAsMetaValue(Object value,
                                    QueryContext context,
                                    MetaField metaField,
                                    boolean forceInit) {

        if (value == null) {
            return null;
        }

        if (value instanceof MetaValue) {
            MetaValue metaValue = (MetaValue) value;
            if (forceInit) {
                metaValue.init(context, metaField);
            }
            return metaValue;
        }

        MetaValue metaValue = toMetaValue(value);
        metaValue.init(context, metaField);

        return metaValue;
    }
}
