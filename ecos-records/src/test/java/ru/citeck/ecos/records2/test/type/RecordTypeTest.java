package ru.citeck.ecos.records2.test.type;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordTypeTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

    private static final String ID = "test";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        setId(ID);
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        EntityRef recRef = EntityRef.create(ID, TestRecord.class.getName());
        EntityRef typeRef = EntityRef.create(ID, TypeInfo.class.getName());

        assertEquals(DataValue.createStr(typeRef.toString()), recordsService.getAtt(recRef, "_type?id"));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<EntityRef> records, MetaField metaField) {
        return records.stream().map(ref -> {
            if (TypeInfo.class.getName().equals(ref.getLocalId())) {
                return new TypeInfo();
            } else if (TestRecord.class.getName().equals(ref.getLocalId())) {
                return new TestRecord();
            }
            return EmptyValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Data
    public static class TestRecord {
        @MetaAtt(".type")
        private EntityRef typeRef = EntityRef.create(ID, TypeInfo.class.getName());
    }

    @Data
    public static class TypeInfo {
        private String name = "Type name";
    }
}
