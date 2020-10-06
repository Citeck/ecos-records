package ru.citeck.ecos.records3.record.operation.meta.schema;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public enum ScalarType {

    ID("?id"),
    STR("?str"),
    DISP("?disp"),
    NUM("?num"),
    ASSOC("?assoc"),
    LOCAL_ID("?localId"),
    BOOL("?bool"),
    JSON("?json");

    @Getter
    private final String schema;

    public static Optional<ScalarType> getBySchema(String schema) {
        return Arrays.stream(values())
            .filter(v -> v.schema.equals(schema))
            .findFirst();
    }
}
