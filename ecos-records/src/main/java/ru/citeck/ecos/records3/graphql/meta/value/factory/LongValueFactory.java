package ru.citeck.ecos.records3.graphql.meta.value.factory;

import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;

public class LongValueFactory implements MetaValueFactory<Long> {

    @Override
    public MetaValue getValue(Long value) {
        return new MetaValue() {
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
