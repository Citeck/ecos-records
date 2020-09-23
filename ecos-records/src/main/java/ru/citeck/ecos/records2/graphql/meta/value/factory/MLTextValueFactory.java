package ru.citeck.ecos.records2.graphql.meta.value.factory;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
public class MLTextValueFactory implements MetaValueFactory<MLText> {

    @Override
    public MetaValue getValue(MLText value) {
        return new Value(value, false);
    }

    @Override
    public List<Class<? extends MLText>> getValueTypes() {
        return Collections.singletonList(MLText.class);
    }

    static class Value implements MetaValue {

        private final MLText value;
        private final boolean closest;

        Value(MLText value, boolean closest) {
            this.value = value;
            this.closest = closest;
        }

        @Override
        public String getString() {
            return value.getClosestValue(QueryContext.getCurrent().getLocale());
        }

        @Override
        public Object getAttribute(@NotNull String name) {
            if (name.equals("closest")) {
                return new Value(value, true);
            }
            if (name.equals("exact")) {
                return new Value(value, false);
            }
            Locale locale = new Locale(name);
            return closest ? value.getClosestValue(locale) : value.get(locale);
        }

        @Override
        public Object getJson() {
            return value.getAsMap();
        }

        @Override
        public boolean has(@NotNull String name) {
            return value.has(new Locale(name));
        }
    }
}
