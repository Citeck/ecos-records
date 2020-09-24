package ru.citeck.ecos.records3.graphql.meta.value.factory;

import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;

public class BooleanValueFactory implements MetaValueFactory<Boolean> {

    @Override
    public MetaValue getValue(Boolean value) {
        return new MetaValue() {
            @Override
            public String getString() {
                return value.toString();
            }

            @Override
            public Boolean getBool() {
                return value;
            }
        };
    }

    @Override
    public List<Class<? extends Boolean>> getValueTypes() {
        return Collections.singletonList(Boolean.class);
    }
}
