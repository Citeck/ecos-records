package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.AndPredicate;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.RecordsServiceImpl;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordsGroupTest extends LocalRecordsDAO
                       implements RecordsQueryLocalDAO,
                                  RecordsQueryWithMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source";

    private RecordsServiceImpl recordsService;
    private PredicateService predicateService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = (RecordsServiceImpl) factory.createRecordsService();
        predicateService = recordsService.getPredicateService();

        recordsService.register(q -> q, "fts", RecordsService.LANGUAGE_PREDICATE);
        recordsService.register(q -> q, RecordsService.LANGUAGE_PREDICATE, "fts");

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery recordsQuery) {

        RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();
        result.setRecords(Collections.singletonList(RecordRef.valueOf("One")));
        result.setHasMore(false);

        return result;
    }

    @Override
    public RecordsQueryResult<Object> getMetaValues(RecordsQuery recordsQuery) {

        if (recordsQuery.getLanguage().equals(DistinctQuery.LANGUAGE)) {

            recordsQuery.getQuery(DistinctQuery.class);
            RecordsQueryResult<Object> result = new RecordsQueryResult<>();
            result.setRecords(Arrays.asList("one", "two"));

            return result;

        } else {

            RecordsQueryResult<Object> result = new RecordsQueryResult<>();

            Predicate predicate = predicateService.readJson(recordsQuery.getQuery());

            predicate = ((AndPredicate) predicate).getPredicates().get(1);

            if (predicate instanceof ValuePredicate) {

                ValuePredicate pred = (ValuePredicate) predicate;

                if (pred.getAttribute().equals("str")) {

                    switch ((String) pred.getValue()) {

                        case "one": {

                            result.setRecords(Arrays.asList(
                                new PojoMeta("one"),
                                new PojoMeta("one"),
                                new PojoMeta("one"),
                                new PojoMeta("one"),
                                new PojoMeta("one"),
                                new PojoMeta("one"),
                                new PojoMeta("one")
                            ));

                            break;
                        }
                        case "two": {

                            result.setRecords(Arrays.asList(
                                new PojoMeta("two"),
                                new PojoMeta("two"),
                                new PojoMeta("two"),
                                new PojoMeta("two123")
                            ));
                            break;
                        }
                    }
                }
            }

            return result;
        }
    }

    @Test
    void test() throws IOException {

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setQuery(ValuePredicate.equal("str", "VALUE"));
        recordsQuery.setSourceId(SOURCE_ID);
        recordsQuery.setLanguage(RecordsService.LANGUAGE_PREDICATE);
        recordsQuery.setGroupBy(Collections.singletonList("str"));

        RecordsQuery baseQuery = objectMapper.readValue(objectMapper.writeValueAsString(recordsQuery), RecordsQuery.class);

        assertResults(recordsService.queryRecords(recordsQuery, Result.class));
        assertEquals(baseQuery, recordsQuery);

        recordsQuery = new RecordsQuery(baseQuery);
        recordsQuery.setLanguage("fts");

        assertResults(recordsService.queryRecords(recordsQuery, Result.class));
    }

    private void assertResults(RecordsQueryResult<Result> records) {

        List<Result> results = records.getRecords();

        assertEquals(2, results.size());

        Result first = results.get(0);
        assertEquals("str", first.getPredicate().getAttribute());
        assertEquals("one", first.getPredicate().getValue());
        assertEquals(21.0, first.getSum());
        assertEquals(7, first.getValues().size());

        Result second = results.get(1);
        assertEquals("str", second.getPredicate().getAttribute());
        assertEquals("two", second.getPredicate().getValue());
        assertEquals(15.0, second.getSum());
        assertEquals(4, second.getValues().size());
    }

    public static class Result {

        @MetaAtt("sum(number)")
        private Double sum;
        private Pred predicate;

        @MetaAtt(".att(n:'values'){atts(n:'records'){str: att(n:'str'){str}, number:att(n:'number'){num}}}")
        private List<Val> values;

        public Double getSum() {
            return sum;
        }

        public void setSum(Double sum) {
            this.sum = sum;
        }

        public Pred getPredicate() {
            return predicate;
        }

        public void setPredicate(Pred predicate) {
            this.predicate = predicate;
        }

        public List<Val> getValues() {
            return values;
        }

        public void setValues(List<Val> values) {
            this.values = values;
        }

        @Override
        public String toString() {
            return "Result{" +
                "sum=" + sum +
                ", predicate=" + predicate +
                ", values=" + values +
                '}';
        }

        public static class Val {

            private String str;
            private String number;

            public String getStr() {
                return str;
            }

            public void setStr(String str) {
                this.str = str;
            }

            public String getNumber() {
                return number;
            }

            public void setNumber(String number) {
                this.number = number;
            }

            @Override
            public String toString() {
                return "Val{" +
                    "str='" + str + '\'' +
                    ", number='" + number + '\'' +
                    '}';
            }
        }

        public class Pred {

            private String attribute;
            private String value;

            public String getAttribute() {
                return attribute;
            }

            public void setAttribute(String attribute) {
                this.attribute = attribute;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return "Pred{" +
                    "attribute='" + attribute + '\'' +
                    ", value='" + value + '\'' +
                    '}';
            }
        }
    }

    public static class PojoMeta {

        private String id;
        private String str;

        PojoMeta(String str) {

            this.id = UUID.randomUUID().toString();
            this.str = str;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public Double getNumber() {
            return (double) str.length();
        }
    }
}
