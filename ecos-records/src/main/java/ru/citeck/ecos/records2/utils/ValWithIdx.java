package ru.citeck.ecos.records2.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Data
@RequiredArgsConstructor
public class ValWithIdx<T> {

    private final T value;
    private final int idx;

    public <V> ValWithIdx<V> map(Function<T, V> mapper) {
        return new ValWithIdx<>(mapper.apply(value), idx);
    }
}
