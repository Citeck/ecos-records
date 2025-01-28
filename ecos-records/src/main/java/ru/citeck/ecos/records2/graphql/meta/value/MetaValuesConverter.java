package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.impl.MetaAttValue;
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttValueUtils;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.atts.value.AttValuesConverter;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

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

        if (value == null || value instanceof EntityRef && EntityRef.isEmpty((EntityRef) value)) {
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

        List<Object> result = AttValueUtils.rawToListWithoutNullValues(rawValue);

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
