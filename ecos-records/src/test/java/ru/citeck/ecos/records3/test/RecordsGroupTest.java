package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.predicate.model.*;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.querylang.QueryLangService;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records3.RecordsServiceImpl;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordsGroupTest extends AbstractRecordsDao
                       implements RecordsQueryDao {

    private static final String SOURCE_ID = "test-source";
    private static final String PROXY_ID = "test-source-proxy";

    @NotNull
    @Override
    public String getId() {
        return SOURCE_ID;
    }

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
    private QueryLangService queryLangService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = (RecordsServiceImpl) factory.getRecordsServiceV1();
        predicateService = factory.getPredicateService();
        queryLangService = factory.getQueryLangService();

        queryLangService.register(q -> q, "fts", PredicateService.LANGUAGE_PREDICATE);
        queryLangService.register(q -> q, PredicateService.LANGUAGE_PREDICATE, "fts");

        recordsService.register(this);
        recordsService.register(new RecordsDaoProxy(PROXY_ID, SOURCE_ID, null));
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
                    String value = valPred.getValue().asText();
                    pred = m -> Objects.equals(m.getStrVal(), value);
                    break;
                case "numKey":
                    double valPredValue = valPred.getValue().asDouble();
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

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery recordsQuery) {

        RecsQueryRes<Object> result = new RecsQueryRes<>();

        Predicate predicate = recordsQuery.getQuery(Predicate.class);
        java.util.function.Predicate<PojoMeta> pojoPredicate = buildPred(predicate);

        int max = recordsQuery.getPage().getMaxItems() >= 0 ? recordsQuery.getPage().getMaxItems() : Integer.MAX_VALUE;

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

    @ParameterizedTest
    @ValueSource(strings = { SOURCE_ID, PROXY_ID } )
    void test(String sourceId) throws IOException {

        Predicate predicate = Predicates.gt("numKey", 4);

        RecordsQuery recordsQuery = RecordsQuery.create()
            .withQuery(predicate)
            .withSourceId(sourceId)
            .withLanguage("fts")
            .withGroupBy(Collections.singletonList("strVal"))
            .build();

        RecordsQuery baseQuery = Json.getMapper().copy(recordsQuery);

        assertResults(recordsService.query(recordsQuery, Result.class), predicate);
        assertEquals(baseQuery, recordsQuery);

        recordsQuery = baseQuery.copy().withLanguage("fts").build();

        assertResults(recordsService.query(recordsQuery, Result.class), predicate);
    }

    private void assertResults(RecsQueryRes<Result> records, Predicate predicate) {

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

            assertEquals(expectedCount, result.getValues().size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { SOURCE_ID/*, PROXY_ID todo */ } )
    void testDistinct(String sourceId) {

        testDistinct(sourceId, Predicates.gt("numKey", 4), p -> p.keyNum > 4);
        testDistinct(sourceId, Predicates.lt("numKey", 4), p -> p.keyNum < 4);
        testDistinct(sourceId, Predicates.ge("numKey", 4), p -> p.keyNum >= 4);
        testDistinct(sourceId, Predicates.le("numKey", 4), p -> p.keyNum <= 4);

        testDistinct(sourceId, Predicates.le("numKey", 10), p -> p.keyNum <= 10);
        testDistinct(sourceId, Predicates.ge("numKey", 10), p -> p.keyNum >= 10);
        testDistinct(sourceId, Predicates.gt("numKey", 10), p -> p.keyNum > 10);
        testDistinct(sourceId, Predicates.lt("numKey", 10), p -> p.keyNum < 10);
        testDistinct(sourceId, Predicates.eq("numKey", 10), p -> p.keyNum == 10);

        testDistinct(sourceId, Predicates.gt("numKey", Integer.MAX_VALUE), p -> false);

        testDistinct(sourceId, Predicates.ge("numKey", 10), p -> false, "unknown");
    }

    private void testDistinct(String sourceId,
                              Predicate predicate,
                              Function<PojoMeta, Boolean> predicateFunc) {

        testDistinct(sourceId, predicate, predicateFunc, "strVal");
    }

    private void testDistinct(String sourceId,
                              Predicate predicate,
                              Function<PojoMeta, Boolean> predicateFunc,
                              String distinctAtt) {

        DistinctQuery distinctQuery = new DistinctQuery();
        distinctQuery.setAttribute(distinctAtt);
        distinctQuery.setLanguage("predicate");
        distinctQuery.setQuery(predicate);

        RecordsQuery recordsQuery = RecordsQuery.create()
            .withSourceId(sourceId)
            .withLanguage(DistinctQuery.LANGUAGE)
            .withQuery(distinctQuery)
            .build();

        RecsQueryRes<DistinctValue> result = recordsService.query(recordsQuery, DistinctValue.class);

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

        @AttName(".str")
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

        @AttName("sum(number)")
        private Double sum;

        @AttName(".atts(n:'values'){strVal: att(n:'strVal'){str}, number:att(n:'number'){num}}")
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
