package ru.citeck.ecos.records3.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonGenerator;
import ecos.com.fasterxml.jackson210.databind.SerializerProvider;
import ecos.com.fasterxml.jackson210.databind.ser.std.StdSerializer;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.commons.json.JsonOptions;
import ru.citeck.ecos.records3.predicate.PredicateUtils;
import ru.citeck.ecos.records3.predicate.model.Predicate;

import java.io.IOException;

public class PredicateJsonSerializer extends StdSerializer<Predicate> {

    private final JsonMapper mapper = Json.newMapper(new JsonOptions.Builder()
        .excludeSerializers(Predicate.class)
        .build());

    public PredicateJsonSerializer() {
        super(Predicate.class);
    }

    @Override
    public void serialize(Predicate predicate,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeTree(mapper.toJson(PredicateUtils.optimize(predicate)));
    }
}
