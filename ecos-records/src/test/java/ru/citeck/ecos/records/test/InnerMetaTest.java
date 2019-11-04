package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.graphql.meta.annotation.DisplayName;
import ru.citeck.ecos.records2.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnerMetaTest extends LocalRecordsDAO implements RecordsMetaLocalDAO<MetaValue> {

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

        JsonNode attribute = recordsService.getAttribute(ref, "field0");
        assertEquals("field0", attribute.asText());

        attribute = recordsService.getAttribute(ref, "inner.field1");
        assertEquals("FIELD1", attribute.asText());

        attribute = recordsService.getAttribute(ref, "display");
        assertEquals("DISPLAY", attribute.asText());

        attribute = recordsService.getAttribute(ref, "innerDisp");
        assertEquals("INNER_DISP", attribute.asText());

        attribute = recordsService.getAttribute(ref, "innerDisp");
        assertEquals("INNER_DISP", attribute.asText());

        attribute = recordsService.getAttribute(ref, "innerStr");
        assertEquals("INNER_STR", attribute.asText());

        attribute = recordsService.getAttribute(ref, "innerField1");
        assertEquals("FIELD1", attribute.asText());

        attribute = recordsService.getAttribute(ref, "innerField1Disp");
        assertEquals("FIELD1", attribute.asText());
    }

    @Override
    public List<MetaValue> getMetaValues(List<RecordRef> records) {
        return records.stream().map(r -> {
            if (r.getId().contains("inner")) {
                return new InnerMeta();
            }
            return new Meta();
        }).collect(Collectors.toList());
    }

    class Meta implements MetaValue {

        private RecordMeta attributes;

        @Override
        public <T extends QueryContext> void init(T context, MetaField field) {
            Map<String, String> atts = field.getInnerAttributesMap();
            atts.put("display", "inner?disp");
            atts.put("innerDisp", ".disp");
            atts.put("innerStr", ".str");
            atts.put("innerField1", "inner.field1");
            atts.put("innerField1Disp", "inner.field1?disp");
            attributes = recordsService.getRawAttributes(RecordRef.create(ID, "inner"), atts);
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            return new InnerMetaValue(attributes.get(name));
        }
    }

    class InnerMeta implements MetaValue {

        InnerInnerMeta inner = new InnerInnerMeta();

        @Override
        public String getId() {
            return "inner";
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            switch (name) {
                case "field0" :
                    return "field0";
                case "inner":
                    return inner;
            }
            return null;
        }

        @Override
        public String getDisplayName() {
            return "INNER_DISP";
        }

        @Override
        public String getString() {
            return "INNER_STR";
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

        @DisplayName
        public String getDisplay() {
            return "DISPLAY";
        }
    }
}