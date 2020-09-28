package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.InnerMetaValue;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnerMetaTest extends LocalRecordsDao implements LocalRecordsMetaDao {

    private static String ID = "";
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

        DataValue attribute = recordsService.getAtt(ref, "field0");
        assertEquals("field0", attribute.asText());

        attribute = recordsService.getAtt(ref, "inner.field1");
        assertEquals("FIELD1", attribute.asText());

        attribute = recordsService.getAtt(ref, "display");
        assertEquals("DISPLAY", attribute.asText());

        attribute = recordsService.getAtt(ref, "innerDisp");
        assertEquals("INNER_DISP", attribute.asText());

        attribute = recordsService.getAtt(ref, "innerDisp");
        assertEquals("INNER_DISP", attribute.asText());

        attribute = recordsService.getAtt(ref, "innerStr");
        assertEquals("INNER_STR", attribute.asText());

        attribute = recordsService.getAtt(ref, "innerField1");
        assertEquals("FIELD1", attribute.asText());

        attribute = recordsService.getAtt(ref, "innerField1Disp");
        assertEquals("FIELD1", attribute.asText());

        attribute = recordsService.getAtt(ref, "innerHasTrue?bool");
        assertTrue(attribute.isBoolean() && attribute.asBoolean());

        attribute = recordsService.getAtt(ref, "innerHasFalse?bool");
        assertTrue(attribute.isBoolean() && !attribute.asBoolean());
    }

    @NotNull
    @Override
    public List<AttValue> getLocalRecordsMeta(@NotNull List<RecordRef> records) {
        return records.stream().map(r -> {
            if (r.getId().contains("inner")) {
                return new InnerMeta();
            }
            return new Meta();
        }).collect(Collectors.toList());
    }

    class Meta implements AttValue {

        private RecordAtts attributes;

        //todo
        /*@Override
        public <T extends QueryContext> void init(T context, SchemaAtt field) {
            Map<String, String> atts = field.getInnerAttributesMap();
            atts.put("display", "inner?disp");
            atts.put("innerDisp", ".disp");
            atts.put("innerStr", ".str");
            atts.put("innerField1", "inner.field1");
            atts.put("innerField1Disp", "inner.field1?disp");
            atts.put("innerHasTrue", ".att(n:\"inner2\"){has(n:\"has_true\")}");
            atts.put("innerHasFalse", ".att(n:\"inner2\"){has(n:\"has_false\")}");
            attributes = recordsService.getRawAttributes(RecordRef.create(ID, "inner"), atts);
        }*/

        @Override
        public Object getAtt(@NotNull String name) {
            return new InnerMetaValue(attributes.get(name));
        }
    }

    class InnerMeta implements AttValue {

        InnerInnerMeta inner = new InnerInnerMeta();
        InnerInnerMeta2 inner2 = new InnerInnerMeta2();

        @Override
        public String getId() {
            return "inner";
        }

        @Override
        public Object getAtt(@NotNull String name) {
            switch (name) {
                case "field0" :
                    return "field0";
                case "inner":
                    return inner;
                case "inner2":
                    return inner2;
            }
            return null;
        }

        @Override
        public String getDispName() {
            return "INNER_DISP";
        }

        @Override
        public String getString() {
            return "INNER_STR";
        }
    }

    public class InnerInnerMeta2 implements AttValue {

        @Override
        public boolean has(@NotNull String name) {
            if ("has_true".equals(name)) {
                return true;
            } else if ("has_false".equals(name)) {
                return false;
            }
            throw new IllegalStateException("Unknown param: " + name);
        }
    }

    public class InnerInnerMeta {

        String field1 = "FIELD1";

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        @MetaAtt(".disp")
        public String getDisplay() {
            return "DISPLAY";
        }
    }
}
