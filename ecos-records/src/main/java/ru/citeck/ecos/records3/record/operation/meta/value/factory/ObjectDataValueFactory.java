package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.Collections;
import java.util.List;

public class ObjectDataValueFactory implements AttValueFactory<ObjectData> {

    @Override
    public AttValue getValue(ObjectData value) {

        return new AttValue() {

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
