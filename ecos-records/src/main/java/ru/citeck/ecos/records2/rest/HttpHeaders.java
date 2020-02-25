package ru.citeck.ecos.records2.rest;

import lombok.Data;

import java.util.*;
import java.util.function.BiConsumer;

@Data
public class HttpHeaders {

    private final Map<String, List<String>> values = new HashMap<>();

    public void forEach(BiConsumer<String, List<String>> action) {
        values.forEach(action);
    }

    public void put(String name, String value) {
        this.values.computeIfAbsent(name, n -> new ArrayList<>()).add(value);
    }

    public void put(String name, List<String> values) {
        this.values.put(name, new ArrayList<>(values));
    }

    public String get(String name, String defaultValue) {
        return values.get(name).stream().findFirst().orElse(defaultValue);
    }

    public List<String> getAll(String name) {
        List<String> result = values.get(name);
        if (result == null) {
            result = Collections.emptyList();
        } else {
            result = Collections.unmodifiableList(result);
        }
        return result;
    }
}