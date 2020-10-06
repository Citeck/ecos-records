package ru.citeck.ecos.records.test.type;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.annotation.AttName;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.EmptyValue;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordTypeTest extends AbstractRecordsDao implements RecordsAttsDao {

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

        RecordRef recRef = RecordRef.create(ID, TestRecord.class.getName());
        RecordRef typeRef = RecordRef.create(ID, TypeInfo.class.getName());

        assertEquals(DataValue.createStr(typeRef.toString()), recordsService.getAtt(recRef, "_type?id"));
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(ref -> {
            if (TypeInfo.class.getName().equals(ref)) {
                return new TypeInfo();
            } else if (TestRecord.class.getName().equals(ref)) {
                return new TestRecord();
            }
            return EmptyValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Data
    public static class TestRecord {
        @AttName(".type")
        private RecordRef typeRef = RecordRef.create(ID, TypeInfo.class.getName());
    }

    @Data
    public static class TypeInfo {
        private String name = "Type name";
    }
}
