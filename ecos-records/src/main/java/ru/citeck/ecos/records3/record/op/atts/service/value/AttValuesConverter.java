package ru.citeck.ecos.records3.record.op.atts.service.value;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records3.record.op.atts.service.value.factory.AttValueFactory;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.meta.AttMetaValue;

import java.util.*;

public class AttValuesConverter {

    private Map<Class<?>, AttValueFactory<Object>> valueFactories = null;
    private final RecordsServiceFactory factory;

    public AttValuesConverter(RecordsServiceFactory factory) {
        this.factory = factory;
    }

    private Map<Class<?>, AttValueFactory<Object>> initFactories() {

        Map<Class<?>, AttValueFactory<Object>> valueFactories = new LinkedHashMap<>();

        for (AttValueFactory<?> valueFactory : factory.getAttValueFactories()) {
            for (Class<?> type : valueFactory.getValueTypes()) {
                @SuppressWarnings("unchecked")
                AttValueFactory<Object> objFactory = (AttValueFactory<Object>) valueFactory;
                valueFactories.put(type, objFactory);
            }
        }

        return valueFactories;
    }

    public AttValue toAttValue(Object value) {

        if (valueFactories == null) {
            valueFactories = initFactories();
        }

        if (value == null || value instanceof RecordRef && RecordRef.isEmpty((RecordRef) value)) {
            return null;
        }
        if (value instanceof AttValue) {
            return (AttValue) value;
        }
        if (value instanceof MetaValue) {
            return new AttMetaValue((MetaValue) value);
        }

        AttValueFactory<Object> factory = valueFactories.get(value.getClass());
        if (factory == null) {
            factory = valueFactories.get(Object.class);
        }

        return factory.getValue(value);
    }
}
