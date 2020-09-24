package ru.citeck.ecos.records3.graphql.meta.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;

public class DataValueMetaFactory implements MetaValueFactory<DataValue> {

    @Override
    public MetaValue getValue(DataValue value) {

        return new MetaValue() {

            @Override
            public String getString() {
                if (value.isValueNode()) {
                    return value.asText();
                }
                return Json.getMapper().toString(value);
            }

            @Override
            public Object getAttribute(@NotNull String name) {
                return value.get(name);
            }
        };
    }

    @Override
    public List<Class<? extends DataValue>> getValueTypes() {
        return Collections.singletonList(DataValue.class);
    }
}
