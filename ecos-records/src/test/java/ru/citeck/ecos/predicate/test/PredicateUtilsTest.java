package ru.citeck.ecos.predicate.test;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.predicate.PredicateUtils;
import ru.citeck.ecos.predicate.model.Predicate;
import ru.citeck.ecos.predicate.model.Predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredicateUtilsTest {


    @Test
    void test() {

        String field0Value = "test-field0";
        int field1Value = 123;
        Double field2Value = 123.123;

        String otherField = "OtherField";
        String otherValue = "OtherValue";

        Predicate pred = Predicates.and(
            Predicates.eq("__field0", field0Value),
            Predicates.eq("__field1", "" + field1Value),
            Predicates.eq("__field2", "" + field2Value),
            Predicates.eq(otherField, otherValue)
        );

        PredDto dto = PredicateUtils.convertToDto(pred, PredDto.class);

        assertEquals(field0Value, dto.field0);
        assertEquals(field1Value, dto.field1);
        assertEquals(field2Value, dto.field2);

        assertEquals(Predicates.eq(otherField, otherValue), dto.filteredPred);


    }


    @Data
    public static class PredDto {

        private String field0;
        private int field1;
        private Double field2;
        private Double field3;

        private Predicate filteredPred;
    }
}
