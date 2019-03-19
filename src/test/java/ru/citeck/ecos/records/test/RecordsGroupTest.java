package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.*;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.RecordsServiceImpl;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private java.util.function.Predicate<PojoMeta> buildPred(Predicate predicate) {

        java.util.function.Predicate<PojoMeta> pred;

        if (predicate instanceof ComposedPredicate) {

            List<Predicate> predicates = ((ComposedPredicate) predicate).getPredicates();

            pred = buildPred(predicates.get(0));
            for (int i = 1; i < predicates.size(); i++) {

                if (predicate instanceof AndPredicate) {
                    pred = pred.and(buildPred(predicates.get(i)));
                } else if (predicate instanceof OrPredicate) {
                    pred = pred.or(buildPred(predicates.get(i)));
                }
            }

        } else if (predicate instanceof EmptyPredicate) {

            pred = m -> m.getStr() == null || m.getStr().isEmpty();

        } else if (predicate instanceof ValuePredicate) {

            ValuePredicate valPred = (ValuePredicate) predicate;

            switch (valPred.getAttribute()) {
                case "str":

                    String value = (String) valPred.getValue();
                    pred = m -> Objects.equals(m.getStr(), value);

                    break;
                case "numKey":

                    Integer valPredValue = (Integer) valPred.getValue();

                    switch (valPred.getType()) {

                        case EQ:

                            pred = m -> m.getKeyNum() == valPredValue;

                            break;

                        case GT:

                            pred = m -> m.getKeyNum() > valPredValue;

                            break;
                        case GE:

                            pred = m -> m.getKeyNum() >= valPredValue;

                            break;
                        case LT:

                            pred = m -> m.getKeyNum() < valPredValue;

                            break;
                        case LE:

                            pred = m -> m.getKeyNum() <= valPredValue;

                            break;

                        default:

                            pred = m -> false;
                    }

                    break;

                default:
                    pred = m -> false;
            }

        } else if (predicate instanceof NotPredicate) {

            pred = buildPred(((NotPredicate) predicate).getPredicate()).negate();

        } else {

            pred = m -> false;
        }

        return pred;
    }

    @Override
    public RecordsQueryResult<Object> getMetaValues(RecordsQuery recordsQuery) {

        RecordsQueryResult<Object> result = new RecordsQueryResult<>();

        Predicate predicate = predicateService.readJson(recordsQuery.getQuery());
        java.util.function.Predicate<PojoMeta> pojoPredicate = buildPred(predicate);

        result.setRecords(Stream.of(
            new PojoMeta("one", 1),
            new PojoMeta("one", 2),
            new PojoMeta("one", 3),
            new PojoMeta("one", 4),
            new PojoMeta("1one55", 200),
            new PojoMeta("1one55", 200),
            new PojoMeta("one", 1000),
            new PojoMeta("1one", 2000),
            new PojoMeta("one12312", 10),
            new PojoMeta("one", 5),
            new PojoMeta("2one", 199),
            new PojoMeta("one", 64),
            new PojoMeta("2one", 21),
            new PojoMeta("two", 123),
            new PojoMeta("two", 124),
            new PojoMeta("two", 6623),
            new PojoMeta("two123", 6342)
        ).filter(pojoPredicate).collect(Collectors.toList()));

        return result;
    }

    @Test
    void test() throws IOException {

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setQuery(Predicates.gt("numKey", 4));
        recordsQuery.setSourceId(SOURCE_ID);
        recordsQuery.setLanguage("fts");
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

        assertEquals(7, results.size());

        for (Result result : results) {
            Result.Pred predicate = result.getPredicate();

            switch (predicate.value) {
                case "1one55":
                    assertEquals(12.0, result.getSum());
                    assertEquals(2,  result.getValues().size());
                    break;
                case "one":
                    assertEquals(9.0, result.getSum());
                    assertEquals(3,  result.getValues().size());
                    break;
                case "1one":
                    assertEquals(4.0, result.getSum());
                    assertEquals(1,  result.getValues().size());
                    break;
                case "one12312":
                    assertEquals(8.0, result.getSum());
                    assertEquals(1,  result.getValues().size());
                    break;
                case "2one":
                    assertEquals(8.0, result.getSum());
                    assertEquals(2,  result.getValues().size());
                    break;
                case "two":
                    assertEquals(9.0, result.getSum());
                    assertEquals(3,  result.getValues().size());
                    break;
                case "two123":
                    assertEquals(6.0, result.getSum());
                    assertEquals(1,  result.getValues().size());
                    break;

            }
        }
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
        private int keyNum;

        PojoMeta(String str, int keyNum) {

            this.id = UUID.randomUUID().toString();
            this.str = str;
            this.keyNum = keyNum;
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

        public int getKeyNum() {
            return keyNum;
        }

        public void setKeyNum(int keyNum) {
            this.keyNum = keyNum;
        }
    }
}
