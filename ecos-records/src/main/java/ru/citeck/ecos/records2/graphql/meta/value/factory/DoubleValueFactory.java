package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.Collections;
import java.util.List;

public class DoubleValueFactory implements MetaValueFactory<Double> {

    @Override
    public MetaValue getValue(Double value) {
        return new MetaValue() {
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
