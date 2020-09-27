package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import ecos.com.fasterxml.jackson210.databind.util.ISO8601Utils;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DateValueFactory implements AttValueFactory<Date> {

    @Override
    public AttValue getValue(Date value) {
        return new DateValue(value);
    }

    @Override
    public List<Class<? extends Date>> getValueTypes() {
        return Collections.singletonList(Date.class);
    }

    static class DateValue implements AttValue {

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
