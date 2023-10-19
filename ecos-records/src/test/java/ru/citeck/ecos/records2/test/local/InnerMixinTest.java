package ru.citeck.ecos.records2.test.local;

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
import ru.citeck.ecos.records2.source.common.AttributesMixin;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InnerMixinTest extends LocalRecordsDao
                            implements LocalRecordsMetaDao<Object> {

    private static final String ID = "mixinSourceId";
    private static final EntityRef DTO_REC_REF = EntityRef.create(ID, "test");
    private static final EntityRef TEST_TYPE0 = EntityRef.create("TYPE", "TEST0");
    private static final EntityRef TEST_TYPE1 = EntityRef.create("TYPE", "TEST1");

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
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
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<EntityRef> records, MetaField metaField) {
        return records.stream().map(ref -> {
            if (ref.getLocalId().equals(DTO_REC_REF.getLocalId())) {
                return new RecRefData(TEST_TYPE0, new RecRefData(TEST_TYPE0, new RecRefData(TEST_TYPE1)));
            }
            return EmptyValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Data
    private static class RecRefData {

        private EntityRef type;
        private RecRefData inner;
        private String computedAtt0 = "computedValue0";
        private String computedAtt1 = "computedValue1";

        public RecRefData(EntityRef type) {
            this.type = type;
        }

        public RecRefData(EntityRef type, RecRefData inner) {
            this.inner = inner;
            this.type = type;
        }

        @MetaAtt(".type")
        public EntityRef getType() {
            return type;
        }
    }
}
