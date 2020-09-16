package ru.citeck.ecos.records2.graphql.meta.value;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class AttFuncValue implements MetaValue {

    private final BiFunction<String, MetaField, ?> impl;

    public AttFuncValue(BiFunction<String, MetaField, ?> impl) {
        this.impl = impl;
    }

    @Override
    public Object getAttribute(@NotNull String name, @NotNull MetaField field) {
        return impl.apply(name, field);
    }
}
