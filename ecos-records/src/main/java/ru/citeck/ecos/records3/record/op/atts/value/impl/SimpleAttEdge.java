package ru.citeck.ecos.records3.record.op.atts.value.impl;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records3.record.op.atts.value.AttEdge;
import ru.citeck.ecos.records3.record.op.atts.value.AttValue;

@Slf4j
public class SimpleAttEdge implements AttEdge {

    private final String name;
    private final AttValue scope;
    private final UncheckedSupplier<Object> getValueFunc;

    public SimpleAttEdge(String name, UncheckedSupplier<Object> getValueFunc) {
        this.name = name;
        this.getValueFunc = getValueFunc;
        this.scope = null;
    }

    public SimpleAttEdge(String name, AttValue scope) {
        this.name = name;
        this.scope = scope;
        this.getValueFunc = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() throws Exception {
        if (getValueFunc != null) {
            return getValueFunc.get();
        } else if (scope != null) {
            return scope.getAtt(getName());
        } else {
            log.warn("Scope and getValueFunc is null");
            return null;
        }
    }
}
