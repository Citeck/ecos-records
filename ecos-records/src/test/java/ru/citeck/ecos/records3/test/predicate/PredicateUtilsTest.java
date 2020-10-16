package ru.citeck.ecos.records3.test.predicate;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PredicateUtilsTest {

    @Test
    void test() {

        String field0Value = "test-field0";
        int field1Value = 123;
        Double field2Value = 123.123;
        Double field22Value = 123.12322;

        String fieldWithPrefixValue = "fieldWithPrefixValue";

        String otherField = "OtherField";
        String otherValue = "OtherValue";

        Predicate pred = Predicates.and(
            Predicates.eq("field0", field0Value),
            Predicates.eq("field1", "" + field1Value),
            Predicates.and(
                Predicates.eq("field2", "" + field2Value),
                Predicates.eq("field22", "" + field22Value)
            ),
            Predicates.or(
                Predicates.eq("field1", "" + field1Value + "123"),
                Predicates.eq("field3", "" + field1Value + "123")
            ),
            Predicates.eq(otherField, otherValue),
            Predicates.eq("__fieldWithPrefix", fieldWithPrefixValue)
        );

        PredDto dto = PredicateUtils.convertToDto(pred, PredDto.class);

        assertEquals(field0Value, dto.field0);
        assertEquals(field1Value, dto.field1);
        assertEquals(field2Value, dto.field2);
        assertEquals(field22Value, dto.field22);
        assertEquals(fieldWithPrefixValue, dto.fieldWithPrefix);
        assertEquals(123123d, dto.field3);
        assertNull(dto.field4);

        assertEquals(Predicates.eq(otherField, otherValue), dto.predicate);

        dto = PredicateUtils.convertToDto(
            Predicates.eq("fieldWithPrefix", fieldWithPrefixValue),
            PredDto.class
        );

        assertEquals(fieldWithPrefixValue, dto.fieldWithPrefix);

        dto = PredicateUtils.convertToDto(pred, PredDto.class, true);
        assertNull(dto.field3);
    }

    @Data
    public static class PredDto {

        private String field0;
        private int field1;
        private Double field2;
        private Double field22;
        private Double field3;
        private String field4;

        private Predicate predicate;

        private String fieldWithPrefix;

        public void set__fieldWithPrefix(String value) {
            fieldWithPrefix = value;
        }
    }
}
