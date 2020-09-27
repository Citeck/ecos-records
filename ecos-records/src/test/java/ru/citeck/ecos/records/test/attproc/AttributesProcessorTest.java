package ru.citeck.ecos.records.test.attproc;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttributesProcessorTest extends LocalRecordsDao implements LocalRecordsMetaDao {

    private static final String ID = "test";
    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordRef ref = RecordRef.create(ID, "test");

        DataValue attribute = recordsService.getAtt(ref, "doubleNum|fmt('000000.00')");
        assertEquals("000123.32", attribute.asText());

        attribute = recordsService.getAtt(ref, "doubleNum?num|fmt('000000.00000')");
        assertEquals("000123.32000", attribute.asText());

        attribute = recordsService.getAtt(ref, "doubleNum?num|fmt('0.0')");
        assertEquals("123.3", attribute.asText());

        attribute = recordsService.getAtt(ref, "date|fmt('YYYY')");
        assertEquals("2020", attribute.asText());

        attribute = recordsService.getAtt(ref, "date|fmt('YYYY.MM.DD')");
        assertEquals("2020.01.01", attribute.asText());

        attribute = recordsService.getAtt(ref, "date|fmt('YYYY.MM.DD___HH')");
        assertEquals("2020.01.01___01", attribute.asText());

        attribute = recordsService.getAtt(ref, "date|fmt('YYYY.MM.DD___HH','','GMT+7:00')");
        assertEquals("2020.01.01___08", attribute.asText());

        attribute = recordsService.getAtt(ref, "date|fmt('YYYY,MM.DD___HH','','GMT+7:00')");
        assertEquals("2020,01.01___08", attribute.asText());

        attribute = recordsService.getAtt(ref, "datee!'_____'|fmt('YYYY.MM.DD___HH','','GMT+7:00')");
        assertEquals("_____", attribute.asText());
    }

    @Test
    void multiProcTest() {

        RecordRef ref = RecordRef.create(ID, "test");

        DataValue attribute = recordsService.getAtt(ref, "date|fmt('YYYY.MM.DD___HH','','GMT+7:00')|presuf('№ ')");
        assertEquals("№ 2020.01.01___08", attribute.asText());

        attribute = recordsService.getAtt(ref, "date|fmt('YYYY.MM.DD___HH','','GMT+7:00')|presuf('№ ', '-ohoh')");
        assertEquals("№ 2020.01.01___08-ohoh", attribute.asText());
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records) {
        return records.stream().map(r -> new Record()).collect(Collectors.toList());
    }

    @Data
    public static class Record {
        private String str = "123";
        private double doubleNum = 123.32;
        private Integer intNum = 123;
        private Date date = Date.from(Instant.parse("2020-01-01T01:01:02.123Z"));
    }
}
