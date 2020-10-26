package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.Collections;
import java.util.List;

public class BooleanValueFactory implements AttValueFactory<Boolean> {

    @Override
    public AttValue getValue(Boolean value) {
        return new AttValue() {
            @Override
            public String asText() {
                return value.toString();
            }

            @Override
            public Boolean asBool() {
                return value;
            }
        };
    }

    @Override
    public List<Class<? extends Boolean>> getValueTypes() {
        return Collections.singletonList(Boolean.class);
    }
}
