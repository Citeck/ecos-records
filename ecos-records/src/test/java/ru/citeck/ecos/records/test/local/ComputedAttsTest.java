package ru.citeck.ecos.records.test.local;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsQueryDao;
import ru.citeck.ecos.records3.type.ComputedAttribute;
import ru.citeck.ecos.records3.type.RecordTypeService;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComputedAttsTest extends LocalRecordsDao
    implements LocalRecordsQueryDao,
    LocalRecordsMetaDao {

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

    private final MLText name0 = DataValue.create("{" +
        "\"ru\": \"Русский0-${idfield}\", " +
        "\"en\":\"English0-${idfield}\"" +
    "}").getAs(MLText.class);

    private final MLText name1 = DataValue.create("{" +
        "\"ru\": \"Русский1-${idfield}\", " +
        "\"en\":\"English1-${idfield}\"" +
        "}").getAs(MLText.class);

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
        RecsQueryRes<RecordMeta> result = recordsService.queryRecords(query, mixinAtts);
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
    public RecsQueryRes<Object> queryLocalRecords(RecordsQuery query) {
        return RecsQueryRes.of(new MetaValueRecord("type0"), new MetaValueRecord("type1"));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records) {
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
        public Object getAttribute(@NotNull String name) throws Exception {

            switch (name) {
                case "strField": return strFieldValue;
                case "intField0": return intField0Value;
                case "intField1": return intField1Value;
                case finalFieldName: return finalFieldValue;
                case "inner": return new InnerData();
                case "idfield": return id;
            }

            return null;
        }

        @Override
        public RecordRef getRecordType() {
            return RecordRef.create("emodel", "type", id);
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
