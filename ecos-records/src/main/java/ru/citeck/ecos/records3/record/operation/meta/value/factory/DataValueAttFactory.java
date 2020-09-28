package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.Collections;
import java.util.List;

public class DataValueAttFactory implements AttValueFactory<DataValue> {

    @Override
    public AttValue getValue(DataValue value) {

        return new AttValue() {

            @Override
            public String getString() {
                if (value.isValueNode()) {
                    return value.asText();
                }
                return Json.getMapper().toString(value);
            }

            @Override
            public Object getAtt(@NotNull String name) {
                return value.get(name);
            }
        };
    }

    @Override
    public List<Class<? extends DataValue>> getValueTypes() {
        return Collections.singletonList(DataValue.class);
    }
}
