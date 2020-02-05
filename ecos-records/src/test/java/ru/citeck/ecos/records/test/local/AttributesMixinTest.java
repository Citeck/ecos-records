package ru.citeck.ecos.records.test.local;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.common.AttributesMixin;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributesMixinTest extends LocalRecordsDAO
                                 implements LocalRecordsQueryWithMetaDAO<Object>,
                                            LocalRecordsMetaDAO<Object> {

    private static final String ID = "mixinSourceId";
    private static final String REC_ID = "rec-id";
    private static final String REC_META_VALUE_ID = "rec-meta-value-id";

    private static final String strFieldValue = "value";
    private static final String strFieldPrefixValue = "prefix-";
    private static final String strFieldValueWithPrefix = strFieldPrefixValue + strFieldValue;
    private static final String strFieldValueWithPrefixName = "valueWithPrefix";

    private static final int intField0Value = 10;
    private static final int intField1Value = 20;
    private static final int intFieldsSum = intField0Value + intField1Value;
    private static final String intFieldsSumName = "intFieldsSumName";

    private static final String finalFieldValue = "some final value";
    private static final String finalFieldName = "finalField";

    private static final String recordRefAttName = "recId";

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

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt);
        RecordsQueryResult<RecordMeta> result = recordsService.queryRecords(query, mixinAtts);
        RecordMeta meta = result.getRecords().get(0);

        assertEquals(NullNode.getInstance(), meta.get(strAtt));
        assertEquals(NullNode.getInstance(), meta.get(intAtt));

        meta = recordsService.getAttributes(RecordRef.create(ID, REC_ID), mixinAtts);

        assertEquals(NullNode.getInstance(), meta.get(strAtt));
        assertEquals(NullNode.getInstance(), meta.get(intAtt));

        MixinWithDto mixinWithDto = new MixinWithDto();
        addAttributesMixin(mixinWithDto);

        checkValidComputedAttributes();
        removeAttributesMixin(mixinWithDto);

        addAttributesMixin(new MixinWithMap());
        checkValidComputedAttributes();

        addAttributesMixin(new MixinWithRecRef());
        checkValidComputedAttributes();

        JsonNode attValue = recordsService.getAttribute(RecordRef.create(ID, REC_META_VALUE_ID), recordRefAttName);
        assertEquals(TextNode.valueOf(REC_META_VALUE_ID), attValue);
    }

    private void checkValidComputedAttributes() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt, finalFieldName);

        RecordsQueryResult<RecordMeta> result = recordsService.queryRecords(query, mixinAtts);

        result.getRecords().forEach(meta -> {

            assertEquals(TextNode.valueOf(finalFieldValue), meta.get(finalFieldName));

            assertEquals(TextNode.valueOf(strFieldValueWithPrefix), meta.get(strAtt));
            assertEquals(DoubleNode.valueOf(intFieldsSum), meta.get(intAtt));

            meta = recordsService.getAttributes(RecordRef.create(ID, REC_ID), mixinAtts);

            assertEquals(TextNode.valueOf(strFieldValueWithPrefix), meta.get(strAtt));
            assertEquals(DoubleNode.valueOf(intFieldsSum), meta.get(intAtt));
        });
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(RecordsQuery query, MetaField field) {
        return RecordsQueryResult.of(new Record(), new MetaValueRecord(REC_META_VALUE_ID));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream().map(r -> {
            if (REC_ID.equals(r.getId())) {
                return new Record();
            } else if (REC_META_VALUE_ID.equals(r.getId())) {
                return new MetaValueRecord(REC_META_VALUE_ID);
            } else {
                return new EmptyRecord();
            }
        }).collect(Collectors.toList());
    }

    @Data
    public static class EmptyRecord {
    }

    @Data
    public static class Record {

        private String strField = strFieldValue;

        private int intField0 = intField0Value;
        private int intField1 = intField1Value;
        private String finalField = finalFieldValue;
    }

    public static class MetaValueRecord implements MetaValue {

        private String id;

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
                case "strField": return strFieldValue;
                case "intField0": return intField0Value;
                case "intField1": return intField1Value;
                case finalFieldName: return finalFieldValue;
            }

            return null;
        }
    }

    public static class MixinWithRecRef implements AttributesMixin<Object, RecordRef> {

        @Override
        public List<String> getAttributesList() {
            return Collections.singletonList(recordRefAttName);
        }

        @Override
        public Object getAttribute(String attribute, RecordRef id, MetaField field) {
            if (attribute.equals(recordRefAttName)) {
                return id.toString();
            }
            return null;
        }

        @Override
        public Object getMetaToRequest() {
            return null;
        }
    }

    public static class MixinWithMap implements AttributesMixin<Map<String, String>, RecordMeta> {

        @Override
        public List<String> getAttributesList() {
            return MixinWithDto.atts;
        }

        @Override
        public Object getAttribute(String attribute, RecordMeta meta, MetaField field) {
            switch (attribute) {
                case strFieldValueWithPrefixName:
                    return strFieldPrefixValue + meta.getStringOrNull("strField");
                case intFieldsSumName:
                    return meta.get("intField0", 0.0) + meta.get("intField1", 0.0);
            }
            return null;
        }

        @Override
        public Map<String, String> getMetaToRequest() {

            Map<String, String> res = new HashMap<>();
            res.put("intField0", "intField0");
            res.put("intField1", "intField1");
            res.put("strField", "strField");

            return res;
        }
    }

    public static class MixinWithDto implements AttributesMixin<Class<MixinWithDto.MixinMeta>, MixinWithDto.MixinMeta> {

        private static final List<String> atts = Arrays.asList(strFieldValueWithPrefixName, intFieldsSumName);

        @Override
        public List<String> getAttributesList() {
            return atts;
        }

        @Override
        public Object getAttribute(String attribute, MixinMeta meta, MetaField field) {
            switch (attribute) {
                case strFieldValueWithPrefixName:
                    return strFieldPrefixValue + meta.str;
                case intFieldsSumName:
                    return meta.intField0 + meta.intField1;
            }
            return null;
        }

        @Override
        public Class<MixinMeta> getMetaToRequest() {
            return MixinMeta.class;
        }

        @Data
        public static class MixinMeta {

            @MetaAtt("strField")
            private String str;

            private int intField0;
            private int intField1;
        }
    }
}
