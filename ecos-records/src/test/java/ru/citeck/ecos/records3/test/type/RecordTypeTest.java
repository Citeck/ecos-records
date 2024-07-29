package ru.citeck.ecos.records3.test.type;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordTypeTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String ID = "test";

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        EntityRef recRef = EntityRef.create(ID, TestRecord.class.getName());
        EntityRef typeRef = EntityRef.create(ID, TypeInfo.class.getName());

        assertEquals(DataValue.createStr(typeRef.toString()), recordsService.getAtt(recRef, "_type?id"));
        assertEquals(DataValue.createStr(typeRef.toString()), recordsService.getAtt(recRef, ".type{id}"));
        assertEquals(DataValue.createStr(typeRef.toString()), recordsService.getAtt(recRef, "?type{id}"));
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(ref -> {
            if (TypeInfo.class.getName().equals(ref)) {
                return new TypeInfo();
            } else if (TestRecord.class.getName().equals(ref)) {
                return new TestRecord();
            }
            return EmptyAttValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Data
    public static class TestRecord {
        @AttName(".type")
        private EntityRef typeRef = EntityRef.create(ID, TypeInfo.class.getName());
    }

    @Data
    public static class TypeInfo {
        private String name = "Type name";
    }
}
