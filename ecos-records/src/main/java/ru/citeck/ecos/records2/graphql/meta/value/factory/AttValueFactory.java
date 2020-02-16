package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.objdata.DataValue;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.Collections;
import java.util.List;

public class AttValueFactory implements MetaValueFactory<DataValue> {

    @Override
    public MetaValue getValue(DataValue value) {

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
    public List<Class<? extends DataValue>> getValueTypes() {
        return Collections.singletonList(DataValue.class);
    }
}
