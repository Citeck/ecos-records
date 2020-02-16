package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.records2.attributes.AttValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.utils.JsonUtils;

import java.util.Collections;
import java.util.List;

public class AttValueFactory implements MetaValueFactory<AttValue> {

    @Override
    public MetaValue getValue(AttValue value) {

        return new MetaValue() {

            @Override
            public String getString() {
                if (value.isNull()) {
                    return null;
                }
                if (value.isValueNode()) {
                    return value.asText();
                }
                return JsonUtils.toString(value);
            }

            @Override
            public Object getAttribute(String name, MetaField field) {
                return value.get(name);
            }
        };
    }

    @Override
    public List<Class<? extends AttValue>> getValueTypes() {
        return Collections.singletonList(AttValue.class);
    }
}
