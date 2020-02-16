package ru.citeck.ecos.records2.predicate.json.std;

import ecos.com.fasterxml.jackson210.core.JsonGenerator;
import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import ecos.com.fasterxml.jackson210.databind.SerializerProvider;
import ecos.com.fasterxml.jackson210.databind.ser.std.StdSerializer;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.io.IOException;

public class PredicateJsonSerializer extends StdSerializer<Predicate> {

    private ObjectMapper mapper = new ObjectMapper();

    public PredicateJsonSerializer() {
        super(Predicate.class);
    }

    @Override
    public void serialize(Predicate predicate,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeTree(mapper.valueToTree(PredicateUtils.optimize(predicate)));
    }
}
