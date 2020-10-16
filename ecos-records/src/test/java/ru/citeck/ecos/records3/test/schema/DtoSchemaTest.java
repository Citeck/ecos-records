package ru.citeck.ecos.records3.test.schema;

import lombok.Data;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.write.AttSchemaWriter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtoSchemaTest {

    @Test
    public void test() {

        RecordsServiceFactory serviceFactory = new RecordsServiceFactory();

        DtoSchemaReader dtoSchemaReader = serviceFactory.getDtoSchemaReader();

        List<SchemaRootAtt> attributesSchema = dtoSchemaReader.read(TestDto.class);
        AttSchemaWriter writer = serviceFactory.getAttSchemaWriter();

        Map<String, String> attributes = writer.writeToMap(attributesSchema);

        assertEquals(4, attributes.size());
        assertEquals(".att(n:\"strField\"){disp}", attributes.get("strField"));
        assertEquals(".att(n:\"inner\"){dumb:att(n:\"_null\"){disp},innerStr:att(n:\"innerStr\"){disp}}", attributes.get("inner"));

        TestDto dto = new TestDto();
        dto.setInner(new Inner());
        dto.getInner().setInnerStr("innerStr-123");
        //dto.getInner().setAa("bb");
        dto.setNumField(123);
        dto.setStrField("strField-123");

        RecordsService recordsService = serviceFactory.getRecordsServiceV1();

        assertEquals(dto, recordsService.getAtts(dto, TestDto.class));
        assertEquals("InnerDisp", serviceFactory.getRecordsServiceV1().getAtt(dto, "inner?disp").asText());
        assertEquals("TestDtoDisp", serviceFactory.getRecordsServiceV1().getAtt(dto, "?disp").asText());

        assertEquals(dto.attWithCustomName, recordsService.getAtts(dto, CustomNameFieldAtt.class).otherField);
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
