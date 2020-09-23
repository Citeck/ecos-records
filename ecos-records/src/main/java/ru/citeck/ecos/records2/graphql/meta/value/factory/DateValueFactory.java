package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ecos.com.fasterxml.jackson210.databind.util.ISO8601Utils;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DateValueFactory implements MetaValueFactory<Date> {

    @Override
    public MetaValue getValue(Date value) {
        return new DateValue(value);
    }

    @Override
    public List<Class<? extends Date>> getValueTypes() {
        return Collections.singletonList(Date.class);
    }

    static class DateValue implements MetaValue {

        private final Date date;

        public DateValue(Date date) {
            this.date = date;
        }

        @Override
        public String getString() {
            return ISO8601Utils.format(date);
        }
    }
}
