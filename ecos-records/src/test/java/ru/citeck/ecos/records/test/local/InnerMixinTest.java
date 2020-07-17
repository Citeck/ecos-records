package ru.citeck.ecos.records.test.local;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.common.AttributesMixin;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.type.ComputedAttribute;
import ru.citeck.ecos.records2.type.RecordTypeService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InnerMixinTest extends LocalRecordsDao
                            implements LocalRecordsMetaDao<Object> {

    private static final String ID = "mixinSourceId";
    private static final RecordRef DTO_REC_REF = RecordRef.create(ID, "test");
    private static final RecordRef TEST_TYPE0 = RecordRef.create("TYPE", "TEST0");
    private static final RecordRef TEST_TYPE1 = RecordRef.create("TYPE", "TEST1");

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory() {
            @Override
            protected RecordTypeService createRecordTypeService() {
                return type -> {
                    if (type.equals(TEST_TYPE0)) {
                        ComputedAttribute att = new ComputedAttribute();
                        att.setId("computed");
                        att.setModel(Collections.singletonMap("<", "computedAtt0"));
                        return Collections.singletonList(att);
                    } else if (type.equals(TEST_TYPE1)) {
                        ComputedAttribute att = new ComputedAttribute();
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
            public Object getAttribute(String attribute, Void meta, MetaField field) {
                if (attribute.equals("innerMeta")) {
                    return "innerValue";
                }
                return null;
            }
            @Override
            public Void getMetaToRequest() {
                return null;
            }
        });
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
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(ref -> {
            if (ref.getId().equals(DTO_REC_REF.getId())) {
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

        @MetaAtt(".type")
        public RecordRef getType() {
            return type;
        }
    }
}
