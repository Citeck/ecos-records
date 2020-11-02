package ru.citeck.ecos.records2.test.predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.Element;
import ru.citeck.ecos.records2.predicate.ElementAttributes;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PredicateMatchTest implements Element, ElementAttributes {

    private Map<String, Object> attributes = new HashMap<>();

    @Test
    void test() {

        attributes.clear();
        attributes.put("a", "aa");
        attributes.put("b", "bb");
        attributes.put("c", "cc");
        attributes.put("d", "dd");

        RecordsServiceFactory factory = new RecordsServiceFactory();
        PredicateService service = factory.getPredicateService();

        Predicate pred = Predicates.or(
            Predicates.eq("a", "aa"),
            Predicates.eq("b", "bb"),
            Predicates.eq("c", "cc"),
            Predicates.eq("d", "dd")
        );

        assertTrue(service.isMatch(this, pred));

        pred = Predicates.or(
            Predicates.eq("a", "aaa"),
            Predicates.eq("b", "bb"),
            Predicates.eq("c", "ccc")
        );

        assertTrue(service.isMatch(this, pred));

        pred = Predicates.and(
            Predicates.eq("a", "aa"),
            Predicates.eq("b", "bb"),
            Predicates.eq("c", "ccc")
        );

        assertFalse(service.isMatch(this, pred));

        pred = Predicates.empty("dd");
        assertTrue(service.isMatch(this, pred));

        attributes.put("dd", null);
        assertTrue(service.isMatch(this, pred));

        attributes.put("dd", "");
        assertTrue(service.isMatch(this, pred));

        attributes.put("dd", "123");
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.or(
            Predicates.eq("a", "aa"),
            Predicates.and(
                Predicates.eq("b", "bb"),
                Predicates.eq("c", "cc")
            ),
            Predicates.eq("d", "dd")
        );

        attributes.clear();
        attributes.put("a", "aaa");
        attributes.put("b", "bb");
        attributes.put("c", "cc");

        assertTrue(service.isMatch(this, pred));

        attributes.put("b", "bbb");
        assertFalse(service.isMatch(this, pred));

        attributes.clear();
        attributes.put("a", "10");

        pred = Predicates.eq("a", 15);
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.eq("a", 10);
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.gt("a", 15);
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.gt("a", 5);
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.gt("a", 10);
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.ge("a", 10);
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.lt("a", 15);
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.lt("a", 5);
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.lt("a", 10);
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.le("a", 10);
        assertTrue(service.isMatch(this, pred));

        attributes.clear();
        attributes.put("a", "2019-02-02T00:00:00Z");

        pred = Predicates.gt("a", Instant.parse("2019-02-01T00:00:00Z"));
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.gt("a", Instant.parse("2019-02-03T00:00:00Z"));
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.gt("a", Instant.parse("2019-02-02T00:00:00Z"));
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.ge("a", Instant.parse("2019-02-02T00:00:00Z"));
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.ge("a", OffsetDateTime.parse("2019-02-02T05:00:00+07:00"));
        assertTrue(service.isMatch(this, pred));

        attributes.clear();
        attributes.put("a", "Some long String");

        pred = Predicates.contains("a", "some");
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.contains("a", "string");
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.contains("a", "longg");
        assertFalse(service.isMatch(this, pred));

        pred = Predicates.eq("a", "Some long String");
        assertTrue(service.isMatch(this, pred));

        pred = Predicates.eq("a", "Some long string");
        assertFalse(service.isMatch(this, pred));
    }

    @Override
    public ElementAttributes getAttributes(List<String> attributes) {
        return this;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
}
