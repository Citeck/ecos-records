package ru.citeck.ecos.records.test.orelse;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.RecordsDaoBuilder;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrElseAttTest  extends AbstractRecordsDao {

    private RecordsService recordsService;

    private final RecordRef TEST_REF = RecordRef.create("test", "test");

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        recordsService.register(RecordsDaoBuilder.create("test")
            .addRecord(TEST_REF, new RecordData())
            .build());
    }

    @Test
    void test() {
        assertEquals(DataValue.createStr("strField"), recordsService.getAtt(TEST_REF, "strField"));
        assertEquals(DataValue.NULL, recordsService.getAtt(TEST_REF, "strNullField"));
        assertEquals(DataValue.create("orElse"), recordsService.getAtt(TEST_REF, "strNullField!'orElse'"));
        assertEquals(DataValue.create("strField"), recordsService.getAtt(TEST_REF, "strNullField!strField"));
        assertEquals(DataValue.create("strField"), recordsService.getAtt(TEST_REF, "strField!strNullField"));

        assertEquals(DataValue.create(1000.0), recordsService.getAtt(TEST_REF, "numberField?num"));
        assertEquals(DataValue.NULL, recordsService.getAtt(TEST_REF, "numberNullField?num"));
        assertEquals(DataValue.create(999.0), recordsService.getAtt(TEST_REF, "numberNullField?num!'999.0'"));
        assertEquals(DataValue.create(1000.0), recordsService.getAtt(TEST_REF, "numberNullField?num!numberField?num"));
        assertEquals(DataValue.create(1000.0), recordsService.getAtt(TEST_REF, "numberField?num!numberNullField?num"));

        assertEquals(DataValue.createStr("2020-01-01T00:00:00Z"), recordsService.getAtt(TEST_REF, "dateField!'2020-01-01T00:00:01Z'"));
        assertEquals(DataValue.NULL, recordsService.getAtt(TEST_REF, "dateNullField"));
        assertEquals(DataValue.createStr("2020-01-01T00:00:01Z"), recordsService.getAtt(TEST_REF, "dateNullField!'2020-01-01T00:00:01Z'"));
        assertEquals(DataValue.createStr("2020-01-01T00:00:00Z"), recordsService.getAtt(TEST_REF, "dateNullField!dateField"));
        assertEquals(DataValue.createStr("2020-01-01T00:00:00Z"), recordsService.getAtt(TEST_REF, "dateField!dateNullField"));

        assertEquals(DataValue.create(true), recordsService.getAtt(TEST_REF, "boolField?bool"));
        assertEquals(DataValue.NULL, recordsService.getAtt(TEST_REF, "boolNullField?bool"));
        assertEquals(DataValue.create(true), recordsService.getAtt(TEST_REF, "boolNullField?bool!boolField?bool"));
        assertEquals(DataValue.create(true), recordsService.getAtt(TEST_REF, "boolNullField?bool!'true'"));
        assertEquals(DataValue.create(true), recordsService.getAtt(TEST_REF, "boolField?bool!boolNullField?bool"));
    }

    @Test
    void longAttsTest() {

        assertEquals(DataValue.create(true), recordsService.getAtt(TEST_REF, "boolNullField?bool!numberNullField?num!strNullField!'true'"));
        assertEquals(DataValue.create(true), recordsService.getAtt(TEST_REF, ".att('boolNullField'){bool}!.att('numberNullField'){num}!.att('strNullField'){disp}!'true'"));
        assertEquals(DataValue.create(false), recordsService.getAtt(TEST_REF, "boolNullField?bool!numberNullField?num!strNullField!'false'"));
        assertEquals(DataValue.create(false), recordsService.getAtt(TEST_REF, ".att('boolNullField'){bool}!.att('numberNullField'){num}!.att('strNullField'){disp}!'false'"));

        assertEquals(DataValue.createStr("strField"), recordsService.getAtt(TEST_REF, "boolNullField?bool!numberNullField?num!strField"));
    }

    @Test
    void innerTest() {
        assertEquals(DataValue.TRUE, recordsService.getAtt(TEST_REF, "meta?as('recordData').boolField?bool"));
        assertEquals(DataValue.TRUE, recordsService.getAtt(TEST_REF, "meta?as('recordData').meta?has('abc')"));
        assertEquals(DataValue.FALSE, recordsService.getAtt(TEST_REF, "meta?as('recordData').meta?has('def')"));
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

