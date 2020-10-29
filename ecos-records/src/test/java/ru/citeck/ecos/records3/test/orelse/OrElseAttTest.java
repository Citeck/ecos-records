package ru.citeck.ecos.records3.test.orelse;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrElseAttTest {

    private RecordsService recordsService;

    private final RecordRef TEST_REF = RecordRef.create("test", "test");

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

        recordsService.register(RecordsDaoBuilder.create("test")
            .addRecord("test", new RecordData())
            .build());
    }

    @Test
    void test() {
        assertAtt("strField", "strField");
        assertAtt(null, "strNullField");
        assertAtt("orElse", "strNullField!'orElse'");
        assertAtt("strField", "strNullField!strField");
        assertAtt("strField", "strField!strNullField");

        assertAtt(1000.0, "numberField?num");
        assertAtt(null, "numberNullField?num");
        assertAtt(999.0, "numberNullField?num!999.0");
        assertAtt(1000.0, "numberNullField?num!numberField?num");
        assertAtt(1000.0, "numberField?num!numberNullField?num");

        assertAtt("2020-01-01T00:00:00Z", "dateField!'2020-01-01T00:00:01Z'");
        assertAtt(null, "dateNullField");
        assertAtt("2020-01-01T00:00:01Z", "dateNullField!'2020-01-01T00:00:01Z'");
        assertAtt("2020-01-01T00:00:00Z", "dateNullField!dateField");
        assertAtt("2020-01-01T00:00:00Z", "dateField!dateNullField");

        assertAtt(true, "boolField?bool");
        assertAtt(null, "boolNullField?bool");
        assertAtt(true, "boolNullField?bool!boolField?bool");
        assertAtt(true, "boolNullField?bool!true");
        assertAtt(true, "boolField?bool!boolNullField?bool");
    }

    @Test
    void longAttsTest() {

        assertAtt(true, ".att('boolNullField'){bool}!.att('numberNullField'){num}!.att('strNullField'){disp}!true");
        assertAtt(true, "boolNullField?bool!numberNullField?num!strNullField!true");
        assertAtt(false, "boolNullField?bool!numberNullField?num!strNullField!false");
        assertAtt(false, ".att('boolNullField'){bool}!.att('numberNullField'){num}!.att('strNullField'){disp}!false");

        assertAtt("strField", "boolNullField?bool!numberNullField?num!strField");
    }

    @Test
    void innerTest() {
        assertAtt(true,"meta?as('recordData').boolField?bool");
        assertAtt(true, "meta?as('recordData').meta?has('abc')");
        assertAtt(false, "meta?as('recordData').meta?has('def')");
    }

    @Test
    void multiOrTest() {
        assertAtt("abc", "strField1!strField1!strField1!'abc'");
        assertAtt("strField", "strField!strField!strField!'abc'");
    }

    void assertAtt(Object expected, String att) {
        assertEquals(DataValue.create(expected), recordsService.getAtt(TEST_REF, att));
    }

    @Data
    public static class RecordData {
        private String strField = "strField";
        private String strNullField = null;
        private Double numberField = 1000.0;
        private Double numberNullField = null;
        private Date dateField = Date.from(Instant.parse("2020-01-01T00:00:00Z"));
        private Date dateNullField = null;
        private Boolean boolField = true;
        private Boolean boolNullField = null;
        private MetaData meta = new MetaData();
    }

    public static class MetaData implements AttValue {

        @Override
        public Object getAs(@NotNull String type) {
            if (type.equals("recordData")) {
                return new RecordData();
            }
            return null;
        }

        @Override
        public boolean has(@NotNull String name) {
            switch (name) {
                case "abc": return true;
                case "def": return false;
            }
            return false;
        }
    }
}

