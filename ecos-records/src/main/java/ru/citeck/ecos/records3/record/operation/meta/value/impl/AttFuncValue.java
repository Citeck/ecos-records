package ru.citeck.ecos.records3.record.operation.meta.value.impl;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

public class AttFuncValue implements AttValue {

    private final UncheckedFunction<String, ?> impl;

    public AttFuncValue(UncheckedFunction<String, ?> impl) {
        this.impl = impl;
    }

    @Override
    public Object getAttribute(@NotNull String name) throws Exception {
        return impl.apply(name);
    }
}
