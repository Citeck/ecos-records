package ru.citeck.ecos.records2.graphql.meta.value.factory;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class ByteArrayValueFactory implements MetaValueFactory<byte[]> {

    @Override
    public MetaValue getValue(byte[] value) {

        return new MetaValue() {

            @Override
            public String getString() {
                return Base64.getEncoder().encodeToString(value);
            }

            @Override
            public Object getAs(@NotNull String type) {
                if ("string".equals(type)) {
                    return new String(value, StandardCharsets.UTF_8);
                }
                return null;
            }

            @Override
            public Object getJson() {
                return Json.getMapper().read(value, JsonNode.class);
            }
        };
    }

    @Override
    public List<Class<? extends byte[]>> getValueTypes() {
        return Collections.singletonList(byte[].class);
    }
}
