package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.Collections;
import java.util.List;

public class IntegerValueFactory implements AttValueFactory<Integer> {

    @Override
    public AttValue getValue(Integer value) {
        return new AttValue() {
            @Override
            public String asText() {
                return value.toString();
            }

            @Override
            public Double asDouble() {
                return Double.valueOf(value);
            }

            @Override
            public Boolean asBool() {
                return value != 0;
            }
        };
    }

    @Override
    public List<Class<? extends Integer>> getValueTypes() {
        return Collections.singletonList(Integer.class);
    }
}
