package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.Collections;
import java.util.List;

public class LongValueFactory implements AttValueFactory<Long> {

    @Override
    public AttValue getValue(Long value) {
        return new AttValue() {
            @Override
            public String getString() {
                return value.toString();
            }

            @Override
            public Double getDouble() {
                return Double.valueOf(value);
            }

            @Override
            public Boolean getBool() {
                return value != 0;
            }
        };
    }

    @Override
    public List<Class<? extends Long>> getValueTypes() {
        return Collections.singletonList(Long.class);
    }
}
