package ru.citeck.ecos.records.test.local;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.records2.type.ComputedAttribute;
import ru.citeck.ecos.records2.type.RecordTypeService;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComputedAttsTest extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<Object>,
    LocalRecordsMetaDAO<Object> {

    private static final String ID = "mixinSourceId";

    private static final String strFieldValue = "value";
    private static final String strFieldValueWithPrefixName = "valueWithPrefix";

    private static final int intField0Value = 10;
    private static final int intField1Value = 20;
    private static final String intFieldsSumName = "intFieldsSumName";

    private static final String innerFieldName = "innerFieldData";

    private static final String finalFieldValue = "some final value";
    private static final String finalFieldName = "finalField";

    private RecordsService recordsService;

    private final List<ComputedAttribute> computedAttributesType0 = new ArrayList<>();
    private final List<ComputedAttribute> computedAttributesType1 = new ArrayList<>();

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory() {
            @Override
            protected RecordTypeService createRecordTypeService() {
                return type -> {
                    if ("type0".equals(type.getId())) {
                        return computedAttributesType0;
                    } else if ("type1".equals(type.getId())) {
                        return computedAttributesType1;
                    } else {
                        throw new IllegalStateException("Type is unknown: " + type);
                    }
                };
            }
        };
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt);
        RecordsQueryResult<RecordMeta> result = recordsService.queryRecords(query, mixinAtts);
        RecordMeta meta = result.getRecords().get(0);

        assertTrue(meta.get(strAtt).isNull());
        assertTrue(meta.get(intAtt).isNull());

        // for int

        ComputedAttribute intCcmputedAtt = new ComputedAttribute();
        Map<String, String> model = new HashMap<>();
        model.put("intField0", "intField0?num");
        model.put("intField1", "intField1?num");
        intCcmputedAtt.setModel(model);
        intCcmputedAtt.setType("script");
        intCcmputedAtt.setConfig(ObjectData.create(
            "{"
            + "\"script\":\""
            + "M.get('intField0').asInt() + M.get('intField1').asInt()"
            + "\"}"
        ));
        intCcmputedAtt.setId(intFieldsSumName);

        computedAttributesType0.add(intCcmputedAtt);

        RecordMeta recMeta0 = recordsService.getAttributes(RecordRef.create(getId(), "type0"), mixinAtts);

        assertTrue(recMeta0.get(strAtt).isNull());
        assertEquals(30, recMeta0.get(intAtt).asInt());

        RecordMeta recMeta1 = recordsService.getAttributes(RecordRef.create(getId(), "type1"), mixinAtts);

        assertTrue(recMeta1.get(strAtt).isNull());
        assertTrue(recMeta1.get(intAtt).isNull());

        // for string

        ComputedAttribute strPrefixAtt = new ComputedAttribute();
        Map<String, String> model1 = new HashMap<>();
        model1.put("strField", "strField");
        strPrefixAtt.setModel(model1);
        strPrefixAtt.setType("script");
        strPrefixAtt.setConfig(ObjectData.create(
            "{"
                + "\"script\":\""
                + "return 'some-prefix-' + M.get('strField').asText() + '-' + M.get('unknown', 'def')"
                + "\"}"
        ));
        strPrefixAtt.setId(strFieldValueWithPrefixName);
        String expectedStrPrefixResult = "some-prefix-" + strFieldValue + "-def";

        computedAttributesType1.add(strPrefixAtt);

        recMeta1 = recordsService.getAttributes(RecordRef.create(getId(), "type1"), mixinAtts);
        assertEquals(expectedStrPrefixResult, recMeta1.get(strAtt, ""));
        assertTrue(recMeta1.get(intAtt).isNull());

        recMeta0 = recordsService.getAttributes(RecordRef.create(getId(), "type0"), mixinAtts);

        assertTrue(recMeta0.get(strAtt).isNull());
        assertEquals(30, recMeta0.get(intAtt).asInt());

        computedAttributesType0.add(strPrefixAtt);

        recMeta0 = recordsService.getAttributes(RecordRef.create(getId(), "type0"), mixinAtts);

        assertEquals(expectedStrPrefixResult, recMeta0.get(strAtt, ""));
        assertEquals(30, recMeta0.get(intAtt).asInt());

        // for inner

        ComputedAttribute innerComputedAtt = new ComputedAttribute();
        model = new HashMap<>();
        model.put("<", "inner.strField");
        innerComputedAtt.setModel(model);
        innerComputedAtt.setId(innerFieldName);

        computedAttributesType0.add(innerComputedAtt);

        DataValue res = recordsService.getAttribute(RecordRef.create(getId(), "type0"), innerFieldName);
        assertEquals("some-inner-value", res.asText());
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(RecordsQuery query, MetaField field) {
        return RecordsQueryResult.of(new MetaValueRecord("type0"), new MetaValueRecord("type1"));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(r -> new MetaValueRecord(r.getId())).collect(Collectors.toList());
    }

    public static class MetaValueRecord implements MetaValue {

        private final String id;

        public MetaValueRecord(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Object getAttribute(String name, MetaField field) throws Exception {

            switch (name) {
                case RecordConstants.ATT_ECOS_TYPE: return RecordRef.create("emodel", "type", id);
                case "strField": return strFieldValue;
                case "intField0": return intField0Value;
                case "intField1": return intField1Value;
                case finalFieldName: return finalFieldValue;
                case "inner": return new InnerData();
            }

            return null;
        }
    }

    @Data
    public static class InnerData {

        private String strField = "some-inner-value";

        private int intField0 = intField0Value;
        private int intField1 = intField1Value;
        private String finalField = finalFieldValue;
    }
}
