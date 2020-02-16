package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.objdata.ObjectData;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.Collections;
import java.util.List;

public class AttributesValueFactory implements MetaValueFactory<ObjectData> {

    @Override
    public MetaValue getValue(ObjectData value) {

        return new MetaValue() {

            @Override
            public String getString() {
                return JsonUtils.toString(value);
            }

            @Override
            public Object getAttribute(String name, MetaField field) {
                return value.get(name);
            }

            @Override
            public Object getJson() {
                return this;
            }
        };
    }

    @Override
    public List<Class<? extends ObjectData>> getValueTypes() {
        return Collections.singletonList(ObjectData.class);
    }
}
