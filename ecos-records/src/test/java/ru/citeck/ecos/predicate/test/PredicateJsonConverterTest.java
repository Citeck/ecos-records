package ru.citeck.ecos.predicate.test;

import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;
import ru.citeck.ecos.records2.RecordsServiceFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PredicateJsonConverterTest {

    private Predicate createDeepPredicate() {
        return Predicates.and(
                    Predicates.not(Predicates.empty("att")),
                    Predicates.equal("att", "Val"),
                    Predicates.and(
                        Predicates.empty("att")
                    ),
                    Predicates.and(
                        Predicates.and(
                            Predicates.not(
                                Predicates.and(
                                    Predicates.and(
                                        Predicates.equal("aa", "bb")
                                    )
                                )
                            )
                        )
                    )
                );
    }

    private Predicate getOptimizedPredicate() {
        return Predicates.and(
            Predicates.not(Predicates.empty("att")),
            Predicates.equal("att", "Val"),
            Predicates.empty("att"),
            Predicates.not(Predicates.equal("aa", "bb"))
        );
    }

    @Test
    void testOptimization() {

        PredicateService predicateService = new RecordsServiceFactory().getPredicateService();
        Predicate deepPredicate = createDeepPredicate();
        Predicate optimizedPredicate = getOptimizedPredicate();

        ObjectNode deepJson = predicateService.writeJson(deepPredicate);
        ObjectNode optimJson = predicateService.writeJson(optimizedPredicate);

        assertEquals(optimJson, deepJson);
        assertEquals(deepPredicate, createDeepPredicate());

        deepJson = JsonNodeFactory.instance.objectNode();
        deepJson.put("t", "and");

        ObjectNode inner0 = JsonNodeFactory.instance.objectNode();
        inner0.put("t", "or");

        ObjectNode inner1 = JsonNodeFactory.instance.objectNode();
        inner1.put("t", "eq");
        inner1.put("att", "name");
        inner1.put("val", "value");

        inner0.withArray("val").add(inner1);
        deepJson.withArray("val").add(inner0);

        Predicate jsonToPred = predicateService.readJson(deepJson);

        assertEquals(Predicates.equal("name", "value"), jsonToPred);
    }
}
