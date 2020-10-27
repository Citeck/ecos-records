package ru.citeck.ecos.records3.test.bean;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeanValueFactoryTest extends AbstractRecordsDao implements RecordsAttsDao {

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
        recordsService = factory.getRecordsServiceV1();
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

    @Nullable
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream()
            .map(r -> {
                if (r.equals("0")) {
                    return new ValueDto0();
                } else if (r.equals("1")) {
                    return new ValueDto1();
                }
                throw new IllegalStateException("Unknown ref: " + r);
            }).collect(Collectors.toList());
    }

    @Data
    public static class ValueDto0 {
        @AttName(".disp")
        private String displayName = "dispName";
        @AttName(".str")
        private String strValue = "strValue";
    }

    public static class ValueDto1 {
        @Override
        public String toString() {
            return "ValueDto2{}";
        }
    }

    public enum TestEnum { FIRST, SECOND }
}
