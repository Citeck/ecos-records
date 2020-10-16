package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

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

            @Override
            public Boolean getBool() {
                return value.asBoolean();
            }
        };
    }

    @Override
    public List<Class<? extends DataValue>> getValueTypes() {
        return Collections.singletonList(DataValue.class);
    }
}
