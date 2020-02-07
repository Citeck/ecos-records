package ru.citeck.ecos.predicate.test;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.PredicateServiceImpl;
import ru.citeck.ecos.predicate.PredicateUtils;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;
import ru.citeck.ecos.predicate.model.ValuePredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredicateTest {

    @Test
    void jsonTest() {

        PredicateService predicateService = new PredicateServiceImpl();

        ValuePredicate valuePredicate = new ValuePredicate();
        valuePredicate.setType(ValuePredicate.Type.LIKE);
        valuePredicate.setValue(12312);
        valuePredicate.setAttribute("TestAtt");

        ObjectNode jsonNode = predicateService.writeJson(valuePredicate);

        assertEquals(valuePredicate.getValue(), jsonNode.get("val").asInt());
        assertEquals(valuePredicate.getType().asString(), jsonNode.get("t").asText());
        assertEquals(valuePredicate.getAttribute(), jsonNode.get("att").asText());

        String pred = "{\n" +
            "   \"t\": \"not-and\",\n" +
            "   \"val\": [\n" +
            "       {\n" +
            "           \"t\": \"not-eq\",\n" +
            "           \"att\": \"cm:title\",\n" +
            "           \"val\": \"Some title\"\n" +
            "       },\n" +
            "       {\n" +
            "           \"t\": \"or\",\n" +
            "           \"val\": [\n" +
            "               {\n" +
            "                   \"t\": \"like\",\n" +
            "                   \"att\": \"\",\n" +
            "                   \"val\": \"\"\n" +
            "               },\n" +
            "               {\n" +
            "                   \"t\": \"starts\",\n" +
            "                   \"att\": \"cm:name\",\n" +
            "                   \"val\": \"starts\"\n" +
            "               },\n" +
            "               {\n" +
            "                   \"t\": \"ends\",\n" +
            "                   \"att\": \"cm:global\",\n" +
            "                   \"val\": \"TestTestTest\"\n" +
            "               },\n" +
            "               {\n" +
            "                   \"t\": \"in\",\n" +
            "                   \"att\": \"cm:name\",\n" +
            "                   \"val\": [\n" +
            "                       \"first\",\n" +
            "                       \"second\",\n" +
            "                       \"third\"\n" +
            "                   ]\n" +
            "               },\n" +
            "               {\n" +
            "                   \"t\": \"not\",\n" +
            "                   \"val\" : {\n" +
            "                       \"t\": \"gt\",\n" +
            "                       \"att\": \"cm:date\",\n" +
            "                       \"val\": \"2018-01-01T00:00:00Z\"\n" +
            "                   }\n" +
            "               },\n" +
            "               {\n" +
            "                   \"t\": \"lt\",\n" +
            "                   \"att\": \"cm:date\",\n" +
            "                   \"val\": \"2018-01-01T00:00:00Z\"\n" +
            "               }\n" +
            "           ]\n" +
            "       }\n" +
            "   ]\n" +
            "}";

        Predicate predicate = predicateService.readJson(pred);
        Predicate predicate2 = predicateService.readJson(predicateService.writeJson(predicate));

        assertEquals(predicate, predicate2);
    }

    @Test
    void mappingTest() {

        Predicate pred = Predicates.and(
            Predicates.or(
                Predicates.eq("test", "test1"),
                Predicates.eq("test2", "test2"),
                Predicates.not(Predicates.eq("test3", "test3")),
                Predicates.not(Predicates.eq("ATest3", "test323")),
                Predicates.or(
                    Predicates.or(
                        Predicates.eq("Field", "test"),
                        Predicates.eq("Field1", "test"),
                        Predicates.eq("Field2", "test"),
                        Predicates.eq("test", "test")
                    )
                ),
                Predicates.and(
                    Predicates.and(
                        Predicates.eq("NotPassed", "value")
                    )
                )
            ),
            Predicates.eq("Field", "value"),
            Predicates.eq("Test", "value2"),
            Predicates.eq("testTest", "value2")
        );

        List<Predicate> testedPredicates = new ArrayList<>();
        Optional<Predicate> filtered = PredicateUtils.filterValuePredicates(pred, p -> {
            testedPredicates.add(p);
            return p.getAttribute().startsWith("test");
        });

        assertEquals(12, testedPredicates.size());

        Predicate expected = Predicates.and(
            Predicates.or(
                Predicates.eq("test", "test1"),
                Predicates.eq("test2", "test2"),
                Predicates.not(Predicates.eq("test3", "test3")),
                Predicates.eq("test", "test")
            ),
            Predicates.eq("testTest", "value2")
        );

        assertEquals(expected, filtered.get());
    }
}
