package ru.citeck.ecos.records2.graphql.meta.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;

public class ObjectDataValueFactory implements MetaValueFactory<ObjectData> {

    @Override
    public MetaValue getValue(ObjectData value) {

        return new MetaValue() {

            @Override
            public String getString() {
                return Json.getMapper().toString(value);
            }

            @Override
            public Object getAttribute(@NotNull String name) {
                return value.get(name);
            }

            @Override
            public Object getJson() {
                return value;
            }
        };
    }

    @Override
    public List<Class<? extends ObjectData>> getValueTypes() {
        return Collections.singletonList(ObjectData.class);
    }
}
