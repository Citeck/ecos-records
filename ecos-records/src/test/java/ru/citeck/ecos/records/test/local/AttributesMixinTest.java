package ru.citeck.ecos.records.test.local;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.record.operation.meta.value.AttEdge;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.common.AttributesMixin;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsQueryDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributesMixinTest extends LocalRecordsDao
                                 implements LocalRecordsQueryDao,
                                            LocalRecordsMetaDao {

    private static final String ID = "mixinSourceId";
    private static final String REC_ID = "rec-id";
    private static final String REC_META_VALUE_ID = "rec-meta-value-id";

    private static final String strFieldValue = "value";
    private static final String strFieldPrefixValue = "prefix-";
    private static final String strFieldValueWithPrefix = strFieldPrefixValue + strFieldValue;
    private static final String strFieldValueWithPrefixName = "valueWithPrefix";

    private static final String intField0Name = "intField0";
    private static final String intField0Title = "intField0Title";
    private static final int intField0Value = 10;
    private static final int intField1Value = 20;
    private static final int intFieldsSum = intField0Value + intField1Value;
    private static final String intFieldsSumName = "intFieldsSumName";

    private static final String finalFieldValue = "some final value";
    private static final String finalFieldName = "finalField";

    private static final String recordRefAttName = "recId";
    private static final String recordRefAttTitle = "recRefTitle";

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

        addAttributesMixin(new MixinForExistingAtt());

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt);
        RecordsQueryRes<RecordAtts> result = recordsService.query(query, mixinAtts);
        RecordAtts meta = result.getRecords().get(0);

        assertTrue(meta.get(strAtt).isNull());
        assertTrue(meta.get(intAtt).isNull());

        meta = recordsService.getAtts(RecordRef.create(ID, REC_ID), mixinAtts);

        assertTrue(meta.get(strAtt).isNull());
        assertTrue(meta.get(intAtt).isNull());

        MixinWithDto mixinWithDto = new MixinWithDto();
        addAttributesMixin(mixinWithDto);

        checkValidComputedAttributes();
        removeAttributesMixin(mixinWithDto);

        addAttributesMixin(new MixinWithMap());
        checkValidComputedAttributes();

        addAttributesMixin(new MixinWithRecRef());
        checkValidComputedAttributes();

        DataValue attValue = recordsService.getAtt(RecordRef.create(ID, REC_META_VALUE_ID), recordRefAttName);
        assertEquals(DataValue.create(REC_META_VALUE_ID), attValue);

        DataValue edgeTitle = recordsService.getAtt(RecordRef.create(ID, REC_META_VALUE_ID), "#" + recordRefAttName + "?title");
        assertEquals(DataValue.create(recordRefAttTitle), edgeTitle);

        MetaWithEdgeForExistingAtt edgeExMeta = recordsService.getAtts(RecordRef.create(ID, REC_META_VALUE_ID), MetaWithEdgeForExistingAtt.class);
        assertEquals(intField0Title, edgeExMeta.fieldTitle);
        assertEquals(intField0Value, edgeExMeta.fieldValue);
    }

    private void checkValidComputedAttributes() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt, finalFieldName);

        RecordsQueryRes<RecordAtts> result = recordsService.query(query, mixinAtts);

        result.getRecords().forEach(meta -> {

            assertEquals(DataValue.create(finalFieldValue), meta.get(finalFieldName));

            assertEquals(DataValue.create(strFieldValueWithPrefix), meta.get(strAtt));
            assertEquals(DataValue.create((double) intFieldsSum), meta.get(intAtt));

            meta = recordsService.getAtts(RecordRef.create(ID, REC_ID), mixinAtts);

            assertEquals(DataValue.create(strFieldValueWithPrefix), meta.get(strAtt));
            assertEquals(DataValue.create((double) intFieldsSum), meta.get(intAtt));
        });
    }

    @Override
    public RecordsQueryRes<Object> queryLocalRecords(RecordsQuery query) {
        return RecordsQueryRes.of(new Record(), new MetaValueRecord(REC_META_VALUE_ID));
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records) {
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

    public static class MetaValueRecord implements AttValue {

        private String id;

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
            }

            return null;
        }
    }

    public static class MixinForExistingAtt implements AttributesMixin<Object, AttValue> {

        @Override
        public List<String> getAttributesList() {
            return Collections.singletonList(intField0Name);
        }

        @Override
        public Object getAttribute(String attribute, AttValue meta) throws Exception {
            return meta.getAttribute(attribute);
        }

        @Override
        public AttEdge getEdge(String attribute, AttValue meta, UncheckedSupplier<AttEdge> base) {
            if (attribute.equals(intField0Name)) {
                return new AttEdge() {
                    @Override
                    public String getName() {
                        return attribute;
                    }

                    @Override
                    public Object getValue() throws Exception {
                        return getAttribute(attribute, meta);
                    }

                    @Override
                    public String getTitle() {
                        return intField0Title;
                    }
                };
            }
            return null;
        }

        @Override
        public Object getMetaToRequest() {
            return null;
        }
    }

    public static class MixinWithRecRef implements AttributesMixin<Object, RecordRef> {

        @Override
        public List<String> getAttributesList() {
            return Collections.singletonList(recordRefAttName);
        }

        @Override
        public Object getAttribute(String attribute, RecordRef id) {
            if (attribute.equals(recordRefAttName)) {
                return id.toString();
            }
            return null;
        }

        @Override
        public AttEdge getEdge(String attribute, RecordRef meta, UncheckedSupplier<AttEdge> base) {
            if (attribute.equals(recordRefAttName)) {
                return new AttEdge() {
                    @Override
                    public String getName() {
                        return attribute;
                    }

                    @Override
                    public Object getValue() throws Exception {
                        return getAttribute(attribute, meta);
                    }

                    @Override
                    public String getTitle() {
                        return recordRefAttTitle;
                    }
                };
            }
            return null;
        }

        @Override
        public Object getMetaToRequest() {
            return null;
        }
    }

    public static class MixinWithMap implements AttributesMixin<Map<String, String>, RecordAtts> {

        @Override
        public List<String> getAttributesList() {
            return MixinWithDto.atts;
        }

        @Override
        public Object getAttribute(String attribute, RecordAtts meta) {
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
        public Object getAttribute(String attribute, MixinMeta meta) {
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

    @Data
    public static class MetaWithEdgeForExistingAtt {
        @MetaAtt(".edge(n:\"" + intField0Name + "\"){val{num}}")
        private int fieldValue;
        @MetaAtt("#" + intField0Name + "?title")
        private String fieldTitle;
    }
}
