package ru.citeck.ecos.records3.record.operation.meta.value;

import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.value.factory.AttValueFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AttValuesConverter {

    private final Map<Class<?>, AttValueFactory<Object>> valueFactories = new ConcurrentHashMap<>();

    public AttValuesConverter(RecordsServiceFactory factory) {

        for (AttValueFactory<?> valueFactory : factory.getMetaValueFactories()) {
            for (Class<?> type : valueFactory.getValueTypes()) {
                @SuppressWarnings("unchecked")
                AttValueFactory<Object> objFactory = (AttValueFactory<Object>) valueFactory;
                valueFactories.put(type, objFactory);
            }
        }
    }

    public AttValue toAttValue(Object value) {

        if (value == null || value instanceof RecordRef && RecordRef.isEmpty((RecordRef) value)) {
            return null;
        }
        if (value instanceof AttValue) {
            return (AttValue) value;
        }

        AttValueFactory<Object> factory = valueFactories.get(value.getClass());
        if (factory == null) {
            factory = valueFactories.get(Object.class);
        }

        return factory.getValue(value);
    }
}
