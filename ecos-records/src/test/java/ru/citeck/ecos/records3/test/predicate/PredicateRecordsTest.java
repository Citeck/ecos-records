package ru.citeck.ecos.records3.test.predicate;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.api.records.PredicateRecords;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PredicateRecordsTest {

    private RecordsService recordsService;
    private RecordsServiceFactory recordsServiceFactory;

    @BeforeAll
    void init() {

        recordsServiceFactory = new RecordsServiceFactory();
        recordsService = recordsServiceFactory.getRecordsServiceV1();

        recordsService.register(RecordsDaoBuilder.create("test")
            .addRecord("aaa", new DataDto("str-value", 0))
            .addRecord("bbb", new DataDto("str2-value", 100))
            .addRecord("ccc", new DataDto("str3-value", 0))
            .build());
    }

    @Test
    void testEmptyOr() {

        DataValue predQuery = DataValue.create("{" +
            "\"predicates\":[{\"t\":\"or\",\"val\":[{\"t\":\"or\",\"val\":[]}]}]," +
            "\"record\":\"test@aaa\"" +
        "}");

        RecordsQuery query = RecordsQuery.create()
            .withQuery(predQuery)
            .withSourceId("predicate")
            .build();

        List<ResultDto> records = recordsService.query(query, ResultDto.class).getRecords();
        assertTrue(records.get(0).result.get(0));
    }

    @Test
    void testEmptyAnd() {

        DataValue predQuery = DataValue.create("{" +
            "\"predicates\":[{\"t\":\"or\",\"val\":[{\"t\":\"and\",\"val\":[]}]}]," +
            "\"record\":\"test@aaa\"" +
        "}");

        RecordsQuery query = RecordsQuery.create()
            .withQuery(predQuery)
            .withSourceId("predicate")
            .build();

        List<ResultDto> records = recordsService.query(query, ResultDto.class).getRecords();
        assertTrue(records.get(0).result.get(0));
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

        List<ResultDto> multiRes = check(
            Arrays.asList("aaa", "bbb"),
            Arrays.asList(
                Predicates.eq("strField", "str-value"),
                Predicates.eq("strField", "str2-value")
            )
        );

        assertEquals(2, multiRes.size());
        assertEquals(2, multiRes.get(0).result.size());
        assertEquals(2, multiRes.get(1).result.size());

        assertTrue(multiRes.get(0).result.get(0));
        assertFalse(multiRes.get(0).result.get(1));

        assertFalse(multiRes.get(1).result.get(0));
        assertTrue(multiRes.get(1).result.get(1));
    }

    @Test
    void contextAttsTest() {

        Map<String, Object> ctxAtts = new HashMap<>();
        ctxAtts.put("ctxatt", RecordRef.valueOf("test@ccc"));

        RequestContext.doWithCtxJ(recordsServiceFactory, b -> b.setCtxAtts(ctxAtts), ctx -> {

            assertTrue(check("aaa", Predicates.eq("$ctxatt.strField", "str3-value")));

            Map<String, Object> newAtts = new HashMap<>();
            newAtts.put("ctxatt", RecordRef.valueOf("test@aaa"));
            newAtts.put("ctxatt2", RecordRef.valueOf("test@bbb"));

            RequestContext.doWithAttsJ(newAtts, () -> {
                assertTrue(check("aaa", Predicates.eq("$ctxatt.strField", "str-value")));
                assertTrue(check("aaa", Predicates.eq("$ctxatt2.strField", "str2-value")));
                assertTrue(check("aaa", Predicates.eq("$ctxatt2.intField", "${$ctxatt2.intField}")));
                return null;
            });

            assertNull(RequestContext.getCurrentNotNull().getCtxAtts().get("ctxatt2"));
            assertTrue(check("aaa", Predicates.eq("$ctxatt.strField", "str3-value")));
            assertFalse(check("aaa", Predicates.eq("$ctxatt2.strField", "str2-value")));

            return null;
        });
    }

    private boolean check(String id, Predicate predicate) {
        return check(Collections.singletonList(id), Collections.singletonList(predicate)).get(0).getResult().get(0);
    }

    private List<ResultDto> check(List<String> id, List<Predicate> predicates) {

        PredicateRecords.PredicateCheckQuery checkQuery = new PredicateRecords.PredicateCheckQuery();
        checkQuery.setPredicates(predicates);
        checkQuery.setRecords(id.stream().map(i -> RecordRef.valueOf("test@" + i)).collect(Collectors.toList()));

        RecordsQuery query = RecordsQuery.create()
            .withQuery(checkQuery)
            .withSourceId("predicate")
            .build();

        return recordsService.query(query, ResultDto.class).getRecords();
    }

    @Data
    public static class ResultDto {
        private RecordRef record;
        private List<Boolean> result;
    }

    @Data
    @RequiredArgsConstructor
    public static class DataDto {
        private final String strField;
        private final Integer intField;
    }
}
