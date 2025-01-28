package ru.citeck.ecos.records3.test.local;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.mixin.AttMixin;
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributesMixinTest extends AbstractRecordsDao
                                 implements RecordsQueryDao,
    RecordsAttsDao {

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

        RecordsQuery query = RecordsQuery.create().withSourceId(ID).build();

        addAttributesMixin(new MixinForExistingAtt());

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt);
        RecsQueryRes<RecordAtts> result = recordsService.query(query, mixinAtts);
        RecordAtts meta = result.getRecords().get(0);

        assertTrue(meta.getAtt(strAtt).isNull());
        assertTrue(meta.getAtt(intAtt).isNull());

        meta = recordsService.getAtts(EntityRef.create(ID, REC_ID), mixinAtts);

        assertTrue(meta.getAtt(strAtt).isNull());
        assertTrue(meta.getAtt(intAtt).isNull());

        MixinWithDto mixinWithDto = new MixinWithDto();
        addAttributesMixin(mixinWithDto);
        addAttributesMixin(new AttMixin() {
            @Override
            public Object getAtt(String path, AttValueCtx value) throws Exception {
                if ("_edge.recId.title".equals(path)) {
                    return recordRefAttTitle;
                }
                return null;
            }
            @Override
            public Collection<String> getProvidedAtts() {
                return Collections.singletonList("_edge.recId.title");
            }
        });

        checkValidComputedAttributes();

        addAttributesMixin(new MixinWithMap());
        checkValidComputedAttributes();

        MixinWithRecRef mixinWithRecRef = new MixinWithRecRef();
        addAttributesMixin(mixinWithRecRef);
        checkValidComputedAttributes();

        DataValue attValue = recordsService.getAtt(EntityRef.create(ID, REC_META_VALUE_ID), recordRefAttName);
        assertEquals(DataValue.create(EntityRef.create(ID, REC_META_VALUE_ID).toString()), attValue);

        DataValue edgeTitle = recordsService.getAtt(
            EntityRef.create(ID, REC_META_VALUE_ID),
            "#" + recordRefAttName + "?title"
        );
        assertEquals(DataValue.create(recordRefAttTitle), edgeTitle);
    }

    private void checkValidComputedAttributes() {

        RecordsQuery query = RecordsQuery.create().withSourceId(ID).build();

        String intAtt = intFieldsSumName + "?num";
        String strAtt = strFieldValueWithPrefixName;

        List<String> mixinAtts = Arrays.asList(strAtt, intAtt, finalFieldName);

        RecsQueryRes<RecordAtts> result = recordsService.query(query, mixinAtts);

        result.getRecords().forEach(meta -> {

            assertEquals(DataValue.create(finalFieldValue), meta.getAtt(finalFieldName));

            assertEquals(DataValue.create(strFieldValueWithPrefix), meta.getAtt(strAtt));
            assertEquals(DataValue.create((double) intFieldsSum), meta.getAtt(intAtt));

            meta = recordsService.getAtts(EntityRef.create(ID, REC_ID), mixinAtts);

            assertEquals(DataValue.create(strFieldValueWithPrefix), meta.getAtt(strAtt));
            assertEquals(DataValue.create((double) intFieldsSum), meta.getAtt(intAtt));
        });
    }

    @Override
    public RecsQueryRes<Object> queryRecords(@NotNull RecordsQuery query) {
        return RecsQueryRes.of(new Record(), new MetaValueRecord(REC_META_VALUE_ID));
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {
        return records.stream().map(r -> {
            if (REC_ID.equals(r)) {
                return new Record();
            } else if (REC_META_VALUE_ID.equals(r)) {
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

        private final String id;

        public MetaValueRecord(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Object getAtt(@NotNull String name) throws Exception {

            switch (name) {
                case "recId": return id;
                case "strField": return strFieldValue;
                case "intField0": return intField0Value;
                case "intField1": return intField1Value;
                case finalFieldName: return finalFieldValue;
            }

            return null;
        }
    }

    public static class MixinForExistingAtt implements AttMixin {

        @Override
        public Object getAtt(String path, AttValueCtx value) throws Exception {
            return value.getAtt(path);
        }

        @Override
        public Collection<String> getProvidedAtts() {
            return Collections.singletonList(intField0Name);
        }
    }

    public static class MixinWithRecRef implements AttMixin {

        @Override
        public Collection<String> getProvidedAtts() {
            return Collections.singletonList(recordRefAttName);
        }

        @Override
        public Object getAtt(String path, AttValueCtx value) throws Exception {
            if (path.equals(recordRefAttName)) {
                return value.getRef().toString();
            }
            return null;
        }
    }

    public static class MixinWithMap implements AttMixin {

        @Override
        public Collection<String> getProvidedAtts() {
            return MixinWithDto.atts;
        }

        @Override
        public Object getAtt(String path, AttValueCtx value) throws Exception {

            Map<String, String> metaAtts = new HashMap<>();
            metaAtts.put("intField0", "intField0");
            metaAtts.put("intField1", "intField1");
            metaAtts.put("strField", "strField");

            ObjectData meta = value.getAtts(metaAtts);

            switch (path) {
                case strFieldValueWithPrefixName:
                    return strFieldPrefixValue + meta.get("strField").asText();
                case intFieldsSumName:
                    return meta.get("intField0", 0.0) + meta.get("intField1", 0.0);
            }
            return null;
        }
    }

    public static class MixinWithDto implements AttMixin {

        private static final List<String> atts = Arrays.asList(strFieldValueWithPrefixName, intFieldsSumName);

        @Override
        public Object getAtt(String path, AttValueCtx value) throws Exception {

            MixinMeta meta = value.getAtts(MixinMeta.class);

            switch (path) {
                case strFieldValueWithPrefixName:
                    return strFieldPrefixValue + meta.str;
                case intFieldsSumName:
                    return meta.intField0 + meta.intField1;
            }
            return null;
        }

        @Override
        public Collection<String> getProvidedAtts() {
            return atts;
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
        @AttName(".edge(n:\"" + intField0Name + "\"){val{num}}")
        private int fieldValue;
        @AttName("#" + intField0Name + "?title")
        private String fieldTitle;
    }
}
