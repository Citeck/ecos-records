package ru.citeck.ecos.records3.test.schema;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DtoSchemaTest {

    @Test
    public void test() {

        RecordsServiceFactory serviceFactory = new RecordsServiceFactory();

        DtoSchemaReader dtoSchemaReader = serviceFactory.getDtoSchemaReader();

        List<SchemaAtt> attributesSchema = dtoSchemaReader.read(TestDto.class);
        AttSchemaWriter writer = serviceFactory.getAttSchemaWriter();

        Map<String, String> attributes = writer.writeToMap(attributesSchema);

        assertEquals(4, attributes.size());
        assertEquals("strField", attributes.get("strField"));
        assertEquals("inner{dumb:_null,innerStr}", attributes.get("inner"));

        TestDto dto = new TestDto();
        dto.setInner(new Inner());
        dto.getInner().setInnerStr("innerStr-123");
        //dto.getInner().setAa("bb");
        dto.setNumField(123);
        dto.setStrField("strField-123");

        RecordsService recordsService = serviceFactory.getRecordsService();

        assertEquals(dto, recordsService.getAtts(dto, TestDto.class));
        assertEquals("InnerDisp", serviceFactory.getRecordsService().getAtt(dto, "inner?disp").asText());
        assertEquals("TestDtoDisp", serviceFactory.getRecordsService().getAtt(dto, "?disp").asText());

        assertEquals(dto.attWithCustomName, recordsService.getAtts(dto, CustomNameFieldAtt.class).otherField);

        List<SchemaAtt> constrAtts = serviceFactory.getDtoSchemaReader().read(ConstructorDtoTest.class);
        assertEquals(3, constrAtts.size());
        assertEquals("field0", constrAtts.get(0).getName());
        assertEquals("?disp", constrAtts.get(0).getInner().get(0).getName());
        assertFalse(constrAtts.get(0).getMultiple());
        assertEquals("fieldArr0", constrAtts.get(1).getName());
        assertTrue(constrAtts.get(1).getMultiple());
        assertEquals("?disp", constrAtts.get(1).getInner().get(0).getName());
        assertEquals("intField", constrAtts.get(2).getName());
        assertFalse(constrAtts.get(2).getMultiple());
        assertEquals("?num", constrAtts.get(2).getInner().get(0).getName());
    }

    @Data
    public static class ConstructorDtoTest {

        private final String field0;
        private final List<String> fieldArr0;
        private final int intField;

        public ConstructorDtoTest(@AttName("field0") String field0,
                                  @AttName("fieldArr0") List<String> fieldArr0,
                                  @AttName("intField") int intField) {
            this.field0 = field0;
            this.fieldArr0 = fieldArr0;
            this.intField = intField;
        }
    }

    @Data
    public static class CustomNameFieldAtt {

        @AttName("custom")
        private String otherField;

        private String _null;
    }

    @Data
    public static class TestDto {

        private String strField;
        private int numField;
        private Inner inner;

        @AttName("custom")
        private String attWithCustomName;

        @AttName("?disp")
        public String getDisplayName() {
            return "TestDtoDisp";
        }
    }

    @Data
    public static class Inner {

        private String innerStr;
        @AttName(RecordConstants.ATT_NULL)
        private String dumb;

        @AttName("?disp")
        public String getDisplayName() {
            return "InnerDisp";
        }
    }
}
