package ru.citeck.ecos.records3.graphql.meta.value.factory;

import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;

public class StringValueFactory implements MetaValueFactory<String> {

    @Override
    public MetaValue getValue(String value) {
        return new MetaValue() {
            @Override
            public String getString() {
                return value;
            }

            @Override
            public Boolean getBool() {
                return Boolean.TRUE.toString().equals(value);
            }

            @Override
            public Double getDouble() {
                return Double.parseDouble(value);
            }
        };
    }

    @Override
    public List<Class<? extends String>> getValueTypes() {
        return Collections.singletonList(String.class);
    }
}
