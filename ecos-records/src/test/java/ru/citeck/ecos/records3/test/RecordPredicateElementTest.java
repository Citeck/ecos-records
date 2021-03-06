package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records2.predicate.element.Element;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordPredicateElementTest extends AbstractRecordsDao implements RecordsAttsDao {

    private PredicateService predicates;
    private RecordsService recordsService;

    private Map<String, Map<String, String>> attributes = new HashMap<>();

    @NotNull
    @Override
    public String getId() {
        return "";
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        predicates = factory.getPredicateService();

        factory.getRecordsServiceV1().register(this);
    }

    @Test
    void testElement() {

        attributes.clear();

        attributes.put("", new HashMap<String, String>() {{
            put("a", "aa");
            put("b", "bb");
        }});

        Element element = new RecordElement(recordsService, RecordRef.create("", ""));

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

    @Test
    void testFilter() {

        attributes.clear();

        attributes.put("0", new HashMap<String, String>() {{
            put("a", "aa");
            put("b", "bb");
        }});

        attributes.put("1", new HashMap<String, String>() {{
            put("a", "aaa");
            put("b", "bb");
        }});

        attributes.put("2", new HashMap<String, String>() {{
            put("a", "aa");
            put("b", "bb");
        }});

        attributes.put("3", new HashMap<String, String>() {{
            put("a", "aaa");
            put("b", "bbb");
        }});

        attributes.put("4", new HashMap<String, String>() {{
            put("a", "aaa");
            put("b", "bbb");
            put("c", "cc");
        }});

        attributes.put("5", new HashMap<String, String>() {{
            put("a", "aaa");
            put("b", "bb");
        }});

        attributes.put("6", new HashMap<String, String>() {{
            put("a", "aaa");
            put("b", "bb");
            put("c", "cc");
        }});

        RecordElements elements = new RecordElements(recordsService, Arrays.asList(
            RecordRef.valueOf("0"),
            RecordRef.valueOf("1"),
            RecordRef.valueOf("2"),
            RecordRef.valueOf("3"),
            RecordRef.valueOf("4"),
            RecordRef.valueOf("5"),
            RecordRef.valueOf("6")
        ));

        Predicate pred = Predicates.and(
            Predicates.eq("a", "aaa"),
            Predicates.eq("b", "bb")
        );

        List<RecordElement> filtered = predicateService.filter(elements, pred);
        assertEquals(3, filtered.size());
        assertEquals(RecordRef.valueOf("1"), filtered.get(0).getRecordRef());
        assertEquals(RecordRef.valueOf("5"), filtered.get(1).getRecordRef());
        assertEquals(RecordRef.valueOf("6"), filtered.get(2).getRecordRef());

        filtered = predicateService.filter(elements, pred, 2);
        assertEquals(2, filtered.size());
        assertEquals(RecordRef.valueOf("1"), filtered.get(0).getRecordRef());
        assertEquals(RecordRef.valueOf("5"), filtered.get(1).getRecordRef());

        pred = Predicates.eq("c", "cc");
        filtered = predicateService.filter(elements, pred);
        assertEquals(2, filtered.size());
        assertEquals(RecordRef.valueOf("4"), filtered.get(0).getRecordRef());
        assertEquals(RecordRef.valueOf("6"), filtered.get(1).getRecordRef());
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream()
                        .map(RecordRef::valueOf)
                      .map(TestValue::new)
                      .collect(Collectors.toList());
    }

    class TestValue implements AttValue {

        RecordRef ref;

        public TestValue(RecordRef ref) {
            this.ref = ref;
        }

        @Override
        public String getId() {
            return ref.getId();
        }

        @Override
        public Object getAtt(@NotNull String name) {
            return new AttValue(attributes.get(ref.getId()).get(name));
        }

        class AttValue implements ru.citeck.ecos.records3.record.atts.value.AttValue {

            private String str;

            public AttValue(String str) {
                this.str = str;
            }

            @Override
            public String asText() {
                return str;
            }

            @Override
            public String getDisplayName() {
                return str + "-disp";
            }
        }
    }
}
