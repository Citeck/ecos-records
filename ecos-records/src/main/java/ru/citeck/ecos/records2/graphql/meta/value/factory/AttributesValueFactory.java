package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.records2.attributes.Attributes;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.Collections;
import java.util.List;

public class AttributesValueFactory implements MetaValueFactory<Attributes> {

    @Override
    public MetaValue getValue(Attributes value) {

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
    public List<Class<? extends Attributes>> getValueTypes() {
        return Collections.singletonList(Attributes.class);
    }
}
