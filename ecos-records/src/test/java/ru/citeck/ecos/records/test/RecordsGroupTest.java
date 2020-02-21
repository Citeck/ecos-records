package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.RecordsServiceImpl;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.*;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordsGroupTest extends LocalRecordsDAO
                       implements RecordsQueryLocalDAO,
                                  RecordsQueryWithMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source";

    private static final List<PojoMeta> VALUES;

    static {
        List<PojoMeta> values = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            values.add(new PojoMeta("one", i));
        }
        values.addAll(Arrays.asList(
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
            new PojoMeta("two123", 6342),
            new PojoMeta(null, 6342)
        ));

        VALUES = Collections.unmodifiableList(values);
    }

    private RecordsServiceImpl recordsService;
    private PredicateService predicateService;
    private QueryLangService queryLangService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = (RecordsServiceImpl) factory.getRecordsService();
        predicateService = factory.getPredicateService();
        queryLangService = factory.getQueryLangService();

        queryLangService.register(q -> q, "fts", PredicateService.LANGUAGE_PREDICATE);
        queryLangService.register(q -> q, PredicateService.LANGUAGE_PREDICATE, "fts");

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

            pred = m -> m.getStrVal() == null || m.getStrVal().isEmpty();

        } else if (predicate instanceof ValuePredicate) {

            ValuePredicate valPred = (ValuePredicate) predicate;

            switch (valPred.getAttribute()) {
                case "strVal":

                    String value = (String) valPred.getValue();
                    pred = m -> Objects.equals(m.getStrVal(), value);

                    break;
                case "numKey":

                    double valPredValue = ((Number) valPred.getValue()).doubleValue();

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

        Predicate predicate = recordsQuery.getQuery(Predicate.class);
        java.util.function.Predicate<PojoMeta> pojoPredicate = buildPred(predicate);

        int max = recordsQuery.getMaxItems() >= 0 ? recordsQuery.getMaxItems() : Integer.MAX_VALUE;

        List<PojoMeta> filteredValues = VALUES.stream()
            .filter(pojoPredicate)
            .collect(Collectors.toList());

        List<Object> resultList = new ArrayList<>();

        for (int i = 0; i < max && i < filteredValues.size(); i++) {
            resultList.add(filteredValues.get(i));
        }

        result.setRecords(resultList);

        return result;
    }

    @Test
    void test() throws IOException {

        Predicate predicate = Predicates.gt("numKey", 4);

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setQuery(predicate);
        recordsQuery.setSourceId(SOURCE_ID);
        recordsQuery.setLanguage("fts");
        recordsQuery.setGroupBy(Collections.singletonList("strVal"));

        RecordsQuery baseQuery = JsonUtils.copy(recordsQuery);

        assertResults(recordsService.queryRecords(recordsQuery, Result.class), predicate);
        assertEquals(baseQuery, recordsQuery);

        recordsQuery = new RecordsQuery(baseQuery);
        recordsQuery.setLanguage("fts");

        assertResults(recordsService.queryRecords(recordsQuery, Result.class), predicate);
    }

    private void assertResults(RecordsQueryResult<Result> records, Predicate predicate) {

        java.util.function.Predicate<PojoMeta> pojoPredicate = buildPred(predicate);
        List<PojoMeta> filteredValues = VALUES.stream()
            .filter(v -> v.strVal != null)
            .filter(pojoPredicate)
            .collect(Collectors.toList());

        List<Result> results = records.getRecords();

        Set<String> expectedDistinctValues = new HashSet<>();
        filteredValues.forEach(v -> expectedDistinctValues.add(v.getStrVal()));

        assertEquals(expectedDistinctValues.size(), results.size());

        for (Result result : results) {

            String value = result.getStrVal();
            Double sum = result.getSum();

            Double expectedSum = filteredValues.stream()
                .filter(v -> v.strVal.equals(value))
                .mapToDouble(PojoMeta::getNumber)
                .sum();

            assertEquals(expectedSum, sum);

            long expectedCount = filteredValues.stream()
                .filter(v -> v.strVal.equals(value))
                .count();

            assertEquals(expectedCount, (long) result.getValues().size());
        }
    }

    @Test
    void testDistinct() {

        testDistinct(Predicates.gt("numKey", 4), p -> p.keyNum > 4);
        testDistinct(Predicates.lt("numKey", 4), p -> p.keyNum < 4);
        testDistinct(Predicates.ge("numKey", 4), p -> p.keyNum >= 4);
        testDistinct(Predicates.le("numKey", 4), p -> p.keyNum <= 4);

        testDistinct(Predicates.le("numKey", 10), p -> p.keyNum <= 10);
        testDistinct(Predicates.ge("numKey", 10), p -> p.keyNum >= 10);
        testDistinct(Predicates.gt("numKey", 10), p -> p.keyNum > 10);
        testDistinct(Predicates.lt("numKey", 10), p -> p.keyNum < 10);
        testDistinct(Predicates.eq("numKey", 10), p -> p.keyNum == 10);

        testDistinct(Predicates.gt("numKey", Integer.MAX_VALUE), p -> false);

        testDistinct(Predicates.ge("numKey", 10), p -> false, "unknown");
    }

    private void testDistinct(Predicate predicate, Function<PojoMeta, Boolean> predicateFunc) {
        testDistinct(predicate, predicateFunc, "strVal");
    }

    private void testDistinct(Predicate predicate, Function<PojoMeta, Boolean> predicateFunc, String distinctAtt) {

        RecordsQuery recordsQuery = new RecordsQuery();
        recordsQuery.setSourceId(SOURCE_ID);

        recordsQuery.setLanguage(DistinctQuery.LANGUAGE);
        DistinctQuery distinctQuery = new DistinctQuery();
        distinctQuery.setAttribute(distinctAtt);
        distinctQuery.setLanguage("predicate");
        distinctQuery.setQuery(predicate);
        recordsQuery.setQuery(distinctQuery);

        RecordsQueryResult<DistinctValue> result = recordsService.queryRecords(recordsQuery, DistinctValue.class);

        Set<String> manualCalculatedDistinct = new HashSet<>();
        VALUES.forEach(v -> {
            if (predicateFunc.apply(v) && v.strVal != null) {
                manualCalculatedDistinct.add(v.strVal);
            }
        });

        Set<String> distinctFromQuery = new HashSet<>();
        result.getRecords().forEach(r -> distinctFromQuery.add(r.value));

        assertEquals(manualCalculatedDistinct, distinctFromQuery);
    }

    public static class DistinctValue {

        @MetaAtt(".str")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "DistinctValue{" +
                "value='" + value + '\'' +
                '}';
        }
    }

    public static class Result {

        @MetaAtt("sum(number)")
        private Double sum;

        @MetaAtt(".atts(n:'values'){strVal: att(n:'strVal'){str}, number:att(n:'number'){num}}")
        private List<Val> values;

        private String strVal;

        public String getStrVal() {
            return strVal;
        }

        public void setStrVal(String strVal) {
            this.strVal = strVal;
        }

        public Double getSum() {
            return sum;
        }

        public void setSum(Double sum) {
            this.sum = sum;
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
                ", values=" + values +
                '}';
        }

        public static class Val {

            private String strVal;
            private String number;

            public String getStrVal() {
                return strVal;
            }

            public void setStrVal(String strVal) {
                this.strVal = strVal;
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
                    "str='" + strVal + '\'' +
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
        private String strVal;
        private int keyNum;

        PojoMeta(String strVal, int keyNum) {

            this.id = UUID.randomUUID().toString();
            this.strVal = strVal;
            this.keyNum = keyNum;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStrVal() {
            return strVal;
        }

        public void setStrVal(String strVal) {
            this.strVal = strVal;
        }

        public Double getNumber() {
            return (double) strVal.length();
        }

        public int getKeyNum() {
            return keyNum;
        }

        public void setKeyNum(int keyNum) {
            this.keyNum = keyNum;
        }
    }
}
