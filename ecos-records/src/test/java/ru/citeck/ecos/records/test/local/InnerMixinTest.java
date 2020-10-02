package ru.citeck.ecos.records.test.local;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.graphql.meta.annotation.AttName;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.EmptyValue;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InnerMixinTest extends AbstractRecordsDao
                            implements RecordsAttsDao {

    private static final String ID = "mixinSourceId";
    private static final RecordRef DTO_REC_REF = RecordRef.create(ID, "test");
    private static final RecordRef TEST_TYPE0 = RecordRef.create("TYPE", "TEST0");
    private static final RecordRef TEST_TYPE1 = RecordRef.create("TYPE", "TEST1");

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        /*RecordsServiceFactory factory = new RecordsServiceFactory() {
            @Override
            protected RecTypeService createRecordTypeService() {
                return type -> {
                    if (type.equals(TEST_TYPE0)) {
                        ComputedAtt att = new ComputedAtt();
                        att.setId("computed");
                        att.setModel(Collections.singletonMap("<", "computedAtt0"));
                        return Collections.singletonList(att);
                    } else if (type.equals(TEST_TYPE1)) {
                        ComputedAtt att = new ComputedAtt();
                        att.setId("computed");
                        att.setModel(Collections.singletonMap("<", "computedAtt1"));
                        return Collections.singletonList(att);
                    }
                    return Collections.emptyList();
                };
            }
        };
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);

        addAttributesMixin(new AttributesMixin<Void, Void>() {
            @Override
            public List<String> getAttributesList() {
                return Collections.singletonList("innerMeta");
            }
            @Override
            public Object getAttribute(String attribute, Void meta) {
                if (attribute.equals("innerMeta")) {
                    return "innerValue";
                }
                return null;
            }
            @Override
            public Void getMetaToRequest() {
                return null;
            }
        });*/
    }

    @Test
    void test() {

        DataValue expected = DataValue.createStr("innerValue");

        DataValue innerRes = recordsService.getAtt(DTO_REC_REF, "innerMeta");
        assertEquals(expected, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.innerMeta");
        assertEquals(expected, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.inner.innerMeta");
        assertEquals(expected, innerRes);

        DataValue computedExpected0 = DataValue.createStr("computedValue0");
        DataValue computedExpected1 = DataValue.createStr("computedValue1");

        innerRes = recordsService.getAtt(DTO_REC_REF, "computed");
        assertEquals(computedExpected0, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.computed");
        assertEquals(computedExpected0, innerRes);

        innerRes = recordsService.getAtt(DTO_REC_REF, "inner.inner.computed");
        assertEquals(computedExpected1, innerRes);
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(ref -> {
            if (ref.equals(DTO_REC_REF.getId())) {
                return new RecRefData(TEST_TYPE0, new RecRefData(TEST_TYPE0, new RecRefData(TEST_TYPE1)));
            }
            return EmptyValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Data
    private static class RecRefData {

        private RecordRef type;
        private RecRefData inner;
        private String computedAtt0 = "computedValue0";
        private String computedAtt1 = "computedValue1";

        public RecRefData(RecordRef type) {
            this.type = type;
        }

        public RecRefData(RecordRef type, RecRefData inner) {
            this.inner = inner;
            this.type = type;
        }

        @AttName(".type")
        public RecordRef getType() {
            return type;
        }
    }
}
