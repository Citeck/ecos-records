package ru.citeck.ecos.records.test.predicate;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.api.records.PredicateRecords;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PredicateRecordsTest {

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        recordsService.register(RecordsDaoBuilder.create("test")
            .addRecord("aaa", new DataDto("str-value", 0))
            .addRecord("bbb", new DataDto("str2-value", 100))
            .build());
    }

    @Test
    void test() {

        assertTrue(check("aaa", Predicates.eq("strField", "str-value")));
        assertFalse(check("bbb", Predicates.eq("strField", "str-value")));
        assertTrue(check("bbb", Predicates.eq("strField", "str2-value")));

        assertFalse(check("aaa", Predicates.eq("intField", 10)));
        assertTrue(check("aaa", Predicates.eq("intField", 0)));
        assertTrue(check("aaa", Predicates.gt("intField", -10)));
        assertFalse(check("aaa", Predicates.gt("intField", 0)));
        assertTrue(check("aaa", Predicates.lt("intField", 10)));

        assertFalse(check("bbb", Predicates.eq("intField", 10)));
        assertTrue(check("bbb", Predicates.eq("intField", 100)));
        assertTrue(check("bbb", Predicates.gt("intField", -10)));
        assertFalse(check("bbb", Predicates.gt("intField", 100)));
        assertTrue(check("bbb", Predicates.lt("intField", 102)));
    }

    private boolean check(String id, Predicate predicate) {

        RecordsQuery query = new RecordsQuery();

        PredicateRecords.PredicateCheckQuery checkQuery = new PredicateRecords.PredicateCheckQuery();
        checkQuery.setPredicate(predicate);
        checkQuery.setRecord(RecordRef.valueOf("test@" + id));
        query.setQuery(checkQuery);
        query.setSourceId("predicate");

        Optional<ResultDto> queryRes = recordsService.queryRecord(query, ResultDto.class);

        assertTrue(queryRes.isPresent(), "id: " + id + " predicate: " + predicate);
        return queryRes.get().result;
    }

    @Data
    public static class ResultDto {
        private RecordRef record;
        private Boolean result;
    }

    @Data
    @RequiredArgsConstructor
    public static class DataDto {
        private final String strField;
        private final Integer intField;
    }
}
