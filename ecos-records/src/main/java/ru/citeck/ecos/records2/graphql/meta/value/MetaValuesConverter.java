package ru.citeck.ecos.records2.graphql.meta.value;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.impl.MetaAttValue;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter;
import ru.citeck.ecos.records3.record.atts.value.HasListView;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @deprecated -> AttValuesConverter
 */
@Deprecated
public class MetaValuesConverter {

    private final AttValuesConverter attValuesConverter;

    public MetaValuesConverter(RecordsServiceFactory factory) {
        attValuesConverter = factory.getAttValuesConverter();
    }

    public MetaValue toMetaValue(Object value) {

        if (value == null || value instanceof RecordRef && RecordRef.isEmpty((RecordRef) value)) {
            return null;
        }
        if (value instanceof MetaValue) {
            return (MetaValue) value;
        }

        AttValue attValue = attValuesConverter.toAttValue(value);
        if (attValue != null) {
            return new MetaAttValue(attValue);
        }
        return null;
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

        } else if (rawValue instanceof HasListView) {

            result = new ArrayList<>(((HasListView<?>) rawValue).getListView());

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
