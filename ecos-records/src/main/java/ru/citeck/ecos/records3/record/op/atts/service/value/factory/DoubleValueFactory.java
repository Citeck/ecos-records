package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.Collections;
import java.util.List;

public class DoubleValueFactory implements AttValueFactory<Double> {

    @Override
    public AttValue getValue(Double value) {
        return new AttValue() {
            @Override
            public String asText() {
                return value.toString();
            }

            @Override
            public Double asDouble() {
                return value;
            }

            @Override
            public Boolean asBool() {
                return value != 0;
            }
        };
    }

    @Override
    public List<Class<? extends Double>> getValueTypes() {
        return Collections.singletonList(Double.class);
    }
}
