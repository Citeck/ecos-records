package ru.citeck.ecos.records3.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Data
@RequiredArgsConstructor
public class ValueWithIdx<T> {

    private final T value;
    private final int idx;

    public <V> ValueWithIdx<V> map(Function<T, V> mapper) {
        return new ValueWithIdx<>(mapper.apply(value), idx);
    }
}
