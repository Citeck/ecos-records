package ru.citeck.ecos.records2.graphql.meta.value;

public class SimpleMetaEdge implements MetaEdge {

    private final String name;
    private final MetaValue scope;

    public SimpleMetaEdge(String name, MetaValue scope) {
        this.name = name;
        this.scope = scope;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() throws Exception {
        return scope.getAttribute(getName());
    }
}
