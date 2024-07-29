package ru.citeck.ecos.records3.test.querylang;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.querylang.QueryLangConverter;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.querylang.QueryWithLang;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryLangTest {

    @Test
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();

        QueryLangService queryLangService = factory.getQueryLangService();

        String fromLang = "predicate";
        String toLang = "string";

        queryLangService.register(new QueryLangConv(), fromLang, toLang);

        Predicate predicate = Predicates.and(
            Predicates.ge("field", 10),
            Predicates.eq("other", "value")
        );

        JsonNode predJson = Json.getMapper().toJson(predicate);

        Optional<Object> converted = queryLangService.convertLang(predJson, fromLang, toLang);

        assertEquals(predicate.toString(), converted.get());

        QueryWithLang converted2 = queryLangService.convertLang(predJson, fromLang,
                                                                Collections.singletonList(toLang)).get();

        assertEquals(toLang, converted2.getLanguage());
        assertEquals(predicate.toString(), converted2.getQuery());
    }

    static class QueryLangConv implements QueryLangConverter<Predicate, String> {

        @Override
        public String convert(Predicate query) {
            return query.toString();
        }
    }
}
