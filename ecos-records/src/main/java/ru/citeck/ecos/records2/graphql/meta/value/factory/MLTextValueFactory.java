package ru.citeck.ecos.records2.graphql.meta.value.factory;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
public class MLTextValueFactory implements MetaValueFactory<MLText> {

    @Override
    public MetaValue getValue(MLText value) {
        return new Value(value);
    }

    @Override
    public List<Class<? extends MLText>> getValueTypes() {
        return Collections.singletonList(MLText.class);
    }

    static class Value implements MetaValue {

        private final MLText value;

        Value(MLText value) {
            this.value = value;
        }

        @Override
        public String getString() {
            return value.getClosestValue(QueryContext.getCurrent().getLocale());
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            return value.getClosestValue(new Locale(name));
        }

        @Override
        public Object getJson() {
            return value.getAsMap();
        }

        @Override
        public boolean has(String name) {
            return value.has(new Locale(name));
        }
    }
}
