package ru.citeck.ecos.records3.graphql.meta.value;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;

public class AttFuncValue implements MetaValue {

    private final UncheckedFunction<String, ?> impl;

    public AttFuncValue(UncheckedFunction<String, ?> impl) {
        this.impl = impl;
    }

    @Override
    public Object getAttribute(@NotNull String name) throws Exception {
        return impl.apply(name);
    }
}
