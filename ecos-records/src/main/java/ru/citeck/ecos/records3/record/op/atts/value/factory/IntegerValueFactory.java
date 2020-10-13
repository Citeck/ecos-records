package ru.citeck.ecos.records3.record.op.atts.value.factory;

import ru.citeck.ecos.records3.record.op.atts.value.AttValue;

import java.util.Collections;
import java.util.List;

public class IntegerValueFactory implements AttValueFactory<Integer> {

    @Override
    public AttValue getValue(Integer value) {
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
    public List<Class<? extends Integer>> getValueTypes() {
        return Collections.singletonList(Integer.class);
    }
}
