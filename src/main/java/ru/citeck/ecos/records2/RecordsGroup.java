package ru.citeck.ecos.records2;

import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

public class RecordsGroup implements MetaValue {

    public static final String FIELD_CHILDREN = "children";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_NAME = "name";

    private String name;
    private Object value;

    public RecordsGroup(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getString() {
        return String.valueOf(value);
    }

    @Override
    public Object getAttribute(String name) throws Exception {

        switch (name) {
            case FIELD_NAME:
                return name;
            case FIELD_VALUE:
                return value;
            case FIELD_CHILDREN:

                break;
        }

        return null;
    }
}
