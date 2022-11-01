package ru.citeck.ecos.records2.test.bean;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.record.atts.schema.ScalarType;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeanValueFactoryTest extends LocalRecordsDao
    implements LocalRecordsMetaDao<Object> {

    private static final String ID = "test";
    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        setId(ID);
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordRef ref0 = RecordRef.create(ID, "0");

        ValueDto0 dto0 = new ValueDto0();

        assertEquals(DataValue.createStr(dto0.getDisplayName()), recordsService.getAtt(ref0, ".disp"));
        assertEquals(DataValue.createStr(dto0.getStrValue()), recordsService.getAtt(ref0, ".str"));

        RecordRef ref1 = RecordRef.create(ID, "1");

        ValueDto1 dto1 = new ValueDto1();

        assertEquals(DataValue.createStr(dto1.toString()), recordsService.getAtt(ref1, ".str"));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(r -> {
                if (r.getId().equals("0")) {
                    return new ValueDto0();
                } else if (r.getId().equals("1")) {
                    return new ValueDto1();
                }
                throw new IllegalStateException("Unknown ref: " + r);
            }).collect(Collectors.toList());
    }

    @Data
    public static class ValueDto0 {
        @MetaAtt(".disp")
        private String displayName = "dispName";
        @MetaAtt(".str")
        private String strValue = "strValue";
    }

    public static class ValueDto1 {
        @Override
        @AttName(ScalarType.STR_SCHEMA)
        public String toString() {
            return "ValueDto2{}";
        }
    }

    public enum TestEnum { FIRST, SECOND }
}
