package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
public class StringValueFactory implements AttValueFactory<String> {

    private final Map<String, Function<String, ?>> converters = new ConcurrentHashMap<>();

    public StringValueFactory() {
        converters.put("ref", RecordRef::valueOf);
    }

    @Override
    public AttValue getValue(String value) {

        return new AttValue() {
            @Override
            public String asText() {
                return value;
            }

            @Override
            public Boolean asBool() {
                return Boolean.TRUE.toString().equals(value);
            }

            @Override
            public Double asDouble() {
                return Double.parseDouble(value);
            }

            @Override
            public Object as(@NotNull String type) {
                Function<String, ?> converter = converters.get(type);
                return converter != null ? converter.apply(value) : null;
            }
        };
    }

    public void addConverter(String type, Function<String, ?> converter) {
        Function<String, ?> currentConverter = this.converters.get(type);
        if (currentConverter != null) {
            log.warn("Converter with type " + type + " (" + currentConverter + ") will be replaced by " + converter);
        }
        this.converters.put(type, converter);
    }

    @Override
    public List<Class<? extends String>> getValueTypes() {
        return Collections.singletonList(String.class);
    }
}
