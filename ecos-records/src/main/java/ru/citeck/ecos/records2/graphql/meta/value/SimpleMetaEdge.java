package ru.citeck.ecos.records2.graphql.meta.value;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;

@Slf4j
public class SimpleMetaEdge implements MetaEdge {

    private final String name;
    private final MetaValue scope;
    private final UncheckedSupplier<Object> getValueFunc;

    public SimpleMetaEdge(String name, UncheckedSupplier<Object> getValueFunc) {
        this.name = name;
        this.getValueFunc = getValueFunc;
        this.scope = null;
    }

    public SimpleMetaEdge(String name, MetaValue scope) {
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
            return scope.getAttribute(getName());
        } else {
            log.warn("Scope and getValueFunc is null");
            return null;
        }
    }
}
