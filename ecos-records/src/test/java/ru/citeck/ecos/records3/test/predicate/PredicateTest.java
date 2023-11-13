package ru.citeck.ecos.records3.test.predicate;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.predicate.model.*;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.PredicateUtils;

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
            "               },\n" +
            "               {" +
            "                   \"t\": \"and\",\n" +
            "                   \"val\": [\n" +
            "                       {\n" +
            "                           \"t\": \"not\",\n" +
            "                           \"val\" : {\n" +
            "                               \"t\": \"gt\",\n" +
            "                               \"att\": \"cm:date\",\n" +
            "                               \"val\": \"2018-01-01T00:11:11Z\"\n" +
            "                           }\n" +
            "                       },\n" +
            "                       {\n" +
            "                           \"t\": \"lt\",\n" +
            "                           \"att\": \"cm:date\",\n" +
            "                           \"val\": \"2018-01-01T00:00:00Z\"\n" +
            "                       }\n" +
            "                   ]" +
            "               }\n" +
            "           ]\n" +
            "       }\n" +
            "   ]\n" +
            "}";

        Predicate predicate = Json.getMapper().convert(pred, Predicate.class);

        List<Predicate> andPredicates = ((AndPredicate) ((NotPredicate) predicate).getPredicate()).getPredicates();
        List<Predicate> orPredicates = ((OrPredicate) andPredicates.get(andPredicates.size() - 1)).getPredicates();
        DataValue value = ((ValuePredicate) ((NotPredicate) ((AndPredicate) orPredicates.get(orPredicates.size() - 1)).getPredicates().get(0)).getPredicate()).getValue();
        assertEquals("2018-01-01T00:11:11Z", value.asText());

        Predicate predicate2 = Json.getMapper().convert(Json.getMapper().toJson(predicate), Predicate.class);

        assertEquals(predicate, predicate2);

        Predicate predicate3 = Json.getMapper().read("" + predicate, Predicate.class);
        assertEquals(predicate, predicate3);
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

        Predicate mappedWithOnlyAnd = PredicateUtils.mapValuePredicates(pred, p -> p, true, false);
        assertEquals(pred, mappedWithOnlyAnd);

        Predicate mappedWithoutOnlyAnd = PredicateUtils.mapValuePredicates(pred, p -> p, false, false);
        assertEquals(pred, mappedWithoutOnlyAnd);

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

    @Test
    void testDtoWithPredicateField() {

        // register serializers
        RecordsServiceFactory services = new RecordsServiceFactory();

        Predicate predicate = Predicates.and(
            Predicates.eq("att", "value"),
            Predicates.eq("att1", "value1"),
            Predicates.not(
                Predicates.not(Predicates.or(
                    Predicates.ge("amount", 100),
                    Predicates.eq("amount1", 222)
                ))
            ),
            Predicates.empty("empty-att"),
            Predicates.not(Predicates.empty("empty-att1"))
        );
        Predicate optimizedPredicate = PredicateUtils.optimize(predicate);
        String predicateStr = Json.getMapper().toString(predicate);

        assertEquals(predicate, Json.getMapper().read(predicate.toString(), Predicate.class));

        assert predicateStr != null;
        String dtoStr = "{\"predicate\":\"" + predicateStr.replace("\"", "\\\"") + "\"}";

        DtoWithPredicateField dto = Json.getMapper().read(dtoStr, DtoWithPredicateField.class);
        assert dto != null;
        assertEquals(optimizedPredicate, dto.predicate);

        String dtoStr2 = "{\"predicate\":\"\"}";
        DtoWithPredicateField dto2 = Json.getMapper().read(dtoStr2, DtoWithPredicateField.class);
        assert dto2 != null;
        assertEquals(VoidPredicate.INSTANCE, dto2.predicate);

        DtoWithPredicateField dto3 = new DtoWithPredicateField();
        Json.getMapper().applyData(dto3, dto);
        assertEquals(optimizedPredicate, dto3.predicate);

        String predicateFromRecordStr = services.getRecordsServiceV1().getAtt(dto, "predicate").asText();
        Predicate predicateFromRecord = Json.getMapper().read(predicateFromRecordStr, Predicate.class);

        assertEquals(optimizedPredicate, predicateFromRecord);
        assertEquals("{}", VoidPredicate.INSTANCE.toString());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DtoWithPredicateField {
        private Predicate predicate;
    }
}
