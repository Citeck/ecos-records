package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.Collections;
import java.util.List;

public class ObjectDataValueFactory implements AttValueFactory<ObjectData> {

    @Override
    public AttValue getValue(ObjectData value) {

        return new AttValue() {

            @Override
            public String asText() {
                return Json.getMapper().toString(value);
            }

            @Override
            public Object getAtt(@NotNull String name) {
                return value.get(name);
            }

            @Override
            public Object asJson() {
                return value;
            }
        };
    }

    @Override
    public List<Class<? extends ObjectData>> getValueTypes() {
        return Collections.singletonList(ObjectData.class);
    }
}
