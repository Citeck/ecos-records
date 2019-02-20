package ru.citeck.ecos.records2.graphql.meta.value;

import java.util.List;
import java.util.function.Supplier;

public class MetaEdge {

    private final String name;
    private final Supplier<List<MetaValue>> valueSupplier;

    public MetaEdge(String name, Supplier<List<MetaValue>>  valueSupplier) {
        this.name = name;
        this.valueSupplier = valueSupplier;
    }

    public String getName() {
        return name;
    }

    public List<MetaValue> getValues() {
        return valueSupplier.get();
    }
}
