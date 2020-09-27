package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.Collections;
import java.util.List;

public class DoubleValueFactory implements AttValueFactory<Double> {

    @Override
    public AttValue getValue(Double value) {
        return new AttValue() {
            @Override
            public String getString() {
                return value.toString();
            }

            @Override
            public Double getDouble() {
                return value;
            }

            @Override
            public Boolean getBool() {
                return value != 0;
            }
        };
    }

    @Override
    public List<Class<? extends Double>> getValueTypes() {
        return Collections.singletonList(Double.class);
    }
}
