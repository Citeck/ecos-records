package ru.citeck.ecos.records3.test.predicate;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredicateTest {

    @BeforeAll
    static void before() {
        new RecordsServiceFactory();
    }

    @Test
    void jsonTest() {

        ValuePredicate valuePredicate = new ValuePredicate();
        valuePredicate.setType(ValuePredicate.Type.LIKE);
        valuePredicate.setValue(12312);
        valuePredicate.setAttribute("TestAtt");

        ObjectNode jsonNode = (ObjectNode) Json.getMapper().toJson(valuePredicate);

        assertEquals(valuePredicate.getValue(), DataValue.create(jsonNode.get("val")));
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

        Predicate predicate = Json.getMapper().convert(pred, Predicate.class);
        Predicate predicate2 = Json.getMapper().convert(Json.getMapper().toJson(predicate), Predicate.class);

        assertEquals(predicate, predicate2);
    }

    @Test
    void voidTest() {

        Predicate pred = Predicates.and(
            Predicates.or(
                VoidPredicate.INSTANCE,
                Predicates.or(
                    Predicates.or(
                        VoidPredicate.INSTANCE,
                        VoidPredicate.INSTANCE,
                        VoidPredicate.INSTANCE
                    )
                ),
                Predicates.and(
                    Predicates.and(
                        Predicates.not(
                            VoidPredicate.INSTANCE
                        )
                    )
                )
            ),
            VoidPredicate.INSTANCE,
            VoidPredicate.INSTANCE
        );

        Predicate pred2 = Json.getMapper().read(Json.getMapper().toString(pred), Predicate.class);

        assertEquals(VoidPredicate.INSTANCE, pred2);
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
