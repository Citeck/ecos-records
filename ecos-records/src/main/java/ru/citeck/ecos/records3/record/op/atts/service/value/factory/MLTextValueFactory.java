package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.request.RequestContext;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
public class MLTextValueFactory implements AttValueFactory<MLText> {

    @Override
    public AttValue getValue(MLText value) {
        return new Value(value, false);
    }

    @Override
    public List<Class<? extends MLText>> getValueTypes() {
        return Collections.singletonList(MLText.class);
    }

    static class Value implements AttValue {

        private final MLText value;
        private final boolean closest;

        Value(MLText value, boolean closest) {
            this.value = value;
            this.closest = closest;
        }

        @Override
        public String asText() {
            return value.getClosestValue(RequestContext.getCurrent().getLocale());
        }

        @Override
        public Object getAtt(@NotNull String name) {
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
        public Object asJson() {
            return value.getAsMap();
        }

        @Override
        public boolean has(@NotNull String name) {
            return value.has(new Locale(name));
        }
    }
}
