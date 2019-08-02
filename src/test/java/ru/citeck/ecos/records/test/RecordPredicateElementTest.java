package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.predicate.Element;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;
import ru.citeck.ecos.records2.RecordElement;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordPredicateElementTest extends LocalRecordsDAO implements RecordsMetaLocalDAO<Object> {

    private PredicateService predicates;
    private RecordsService recordsService;

    private Map<String, String> attributes = new HashMap<>();

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();

        predicates = factory.getPredicateService();

        setId("test");
        recordsService.register(this);
    }

    @Test
    void test() {

        attributes.clear();
        attributes.put("a", "aa");
        attributes.put("b", "bb");

        Element element = new RecordElement(recordsService, RecordRef.create("test", ""));

        Predicate pred = Predicates.eq("a", "aa");
        assertTrue(predicates.isMatch(element, pred));
        pred = Predicates.eq("a", "aaa");
        assertFalse(predicates.isMatch(element, pred));

        pred = Predicates.and(
            Predicates.eq("a", "aa"),
            Predicates.eq("b", "bb")
        );
        assertTrue(predicates.isMatch(element, pred));
    }

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        return records.stream().map(TestValue::new).collect(Collectors.toList());
    }

    class TestValue implements MetaValue {

        RecordRef ref;

        public TestValue(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public String getId() {
            return ref.toString();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            return new AttValue(attributes.get(name));
        }

        class AttValue implements MetaValue {

            private String str;

            public AttValue(String str) {
                this.str = str;
            }

            @Override
            public String getString() {
                return str;
            }

            @Override
            public String getDisplayName() {
                return str + "-disp";
            }
        }
    }
}
