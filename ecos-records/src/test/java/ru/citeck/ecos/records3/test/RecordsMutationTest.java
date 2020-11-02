package ru.citeck.ecos.records3.test;

import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsMutationTest extends AbstractRecordsDao
    implements RecordMutateDtoDao<RecordsMutationTest.TestDto> {

    private static final String SOURCE_ID = "test-source-id";
    private static final RecordRef TEST_REF = RecordRef.create(SOURCE_ID, "TEST_REC_ID");

    private static final String ALIAS_VALUE = "ALIAS";

    private RecordsService recordsService;

    private Map<RecordRef, TestDto> valuesToMutate;
    private Map<RecordRef, TestDto> newValues;

    @NotNull
    @Override
    public String getId() {
        return SOURCE_ID;
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);

        valuesToMutate = Collections.singletonMap(TEST_REF, new TestDto(TEST_REF));
        newValues = new HashMap<>();
    }

    @Test
    void test() {

        RecordAtts meta = new RecordAtts(TEST_REF);
        meta.setAtt("field", "test");
        meta.setAtt("field0", "test0");
        meta.setAtt("field1", "test1");
        meta.setAtt("bool", true);

        recordsService.mutate(meta);

        assertEquals("test", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1", valuesToMutate.get(TEST_REF).getField1());
        assertTrue(valuesToMutate.get(TEST_REF).isBool());

        meta = new RecordAtts(TEST_REF);
        meta.setAtt("field{.str}", "__test__");
        meta.setAtt("field0{.json}", "__test0__");
        meta.setAtt("field1{.disp}", "__test1__");
        meta.setAtt("bool{.bool}", false);

        recordsService.mutate(meta);

        assertEquals("__test__", valuesToMutate.get(TEST_REF).getField());
        assertEquals("__test0__", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("__test1__", valuesToMutate.get(TEST_REF).getField1());
        assertFalse(valuesToMutate.get(TEST_REF).isBool());

        meta = new RecordAtts(TEST_REF);
        meta.setAtt(".att(n:\"field\"){str}", "test__");
        meta.setAtt(".att(n:\"field0\"){json}", "test0__");
        meta.setAtt(".att(n:\"field1\"){disp}", "test1__");
        meta.setAtt(".att(n:\"bool\"){bool}", true);

        recordsService.mutate(meta);

        assertEquals("test__", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0__", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1__", valuesToMutate.get(TEST_REF).getField1());
        assertTrue(valuesToMutate.get(TEST_REF).isBool());

        meta = new RecordAtts(TEST_REF);
        meta.setAtt(".att(\"field\"){str}", "test____");
        meta.setAtt(".att(\"field0\"){json}", "test0____");
        meta.setAtt(".att('field1'){disp}", "test1____");
        meta.setAtt(".att('bool'){bool}", false);

        recordsService.mutate(meta);

        assertEquals("test____", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0____", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1____", valuesToMutate.get(TEST_REF).getField1());
        assertFalse(valuesToMutate.get(TEST_REF).isBool());

        assertEquals(0, newValues.size());

        RecordRef newRef = RecordRef.create(SOURCE_ID, "newRef");
        meta = new RecordAtts(newRef);
        meta.setAtt("field", "test");
        meta.setAtt("field0", "test0");
        meta.setAtt("field1", "test1");
        meta.setAtt("bool", false);

        recordsService.mutate(meta);

        assertEquals(1, newValues.size());

        TestDto newDto = newValues.values().stream().findFirst().get();
        assertEquals(newRef.toString() + "-new", newDto.getRef().toString());
        assertEquals("test", newDto.getField());
        assertEquals("test0", newDto.getField0());
        assertEquals("test1", newDto.getField1());
        assertFalse(newDto.isBool());

        newValues.clear();

        RecordRef withInnerRef = RecordRef.create(SOURCE_ID, "newRefWithInner");
        RecordAtts newWithInner = new RecordAtts(withInnerRef);
        newWithInner.setAtt("field", "value123");

        RecordRef innerRef = RecordRef.create(SOURCE_ID, "newInner");

        RecordAtts newInner = new RecordAtts(innerRef);
        newInner.setAtt(RecordConstants.ATT_ALIAS, ALIAS_VALUE);
        newWithInner.setAtt(".att(n:\"assoc0\"){assoc}", ALIAS_VALUE);
        newWithInner.setAtt(".atts(n:\"assoc1\"){assoc}", ALIAS_VALUE);
        ArrayNode innerArrayNode = JsonNodeFactory.instance.arrayNode();
        innerArrayNode.add(ALIAS_VALUE);
        newWithInner.setAtt("assocArr?assoc", innerArrayNode);

        recordsService.mutate(Arrays.asList(newWithInner, newInner));

        assertEquals(2, newValues.size());

        RecordRef newWithInnerRef = RecordRef.valueOf(withInnerRef + "-new");
        TestDto withInnerDto = newValues.get(newWithInnerRef);
        String newInnerRef = innerRef.toString() + "-new";
        assertEquals(newInnerRef, withInnerDto.getAssoc0());
        assertEquals(newInnerRef, withInnerDto.getAssoc1());

        assertEquals(1, withInnerDto.getAssocArr().size());
        assertEquals(newInnerRef, withInnerDto.getAssocArr().get(0));
    }

    @Override
    public TestDto getRecToMutate(@NotNull String recordId) {
        RecordRef ref = RecordRef.create(getId(), recordId);
        TestDto testDto = valuesToMutate.get(ref);
        return testDto == null ? new TestDto(RecordRef.valueOf(ref + "-new")) : new TestDto(testDto);
    }

    @Override
    public String saveMutatedRec(TestDto record) {
        TestDto testDto = valuesToMutate.get(record.getRef());
        if (testDto != null) {
            testDto.set(record);
        } else {
            newValues.put(record.getRef(), record);
        }
        return record.getRef().getId();
    }

    public static class TestDto {

        RecordRef ref;

        String field;
        String field0;
        String field1;
        boolean bool;

        String assoc0;
        String assoc1;
        List<String> assocArr;

        TestDto(RecordRef ref) {
            this.ref = ref;
        }

        TestDto(TestDto other) {
            set(other);
        }

        public void set(TestDto other) {
            ref = other.ref;
            field = other.field;
            field0 = other.field0;
            field1 = other.field1;
            bool = other.bool;
        }

        public boolean isBool() {
            return bool;
        }

        public void setBool(boolean bool) {
            this.bool = bool;
        }

        public RecordRef getRef() {
            return ref;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getField0() {
            return field0;
        }

        public void setField0(String field0) {
            this.field0 = field0;
        }

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getAssoc0() {
            return assoc0;
        }

        public void setAssoc0(String assoc0) {
            this.assoc0 = assoc0;
        }

        public String getAssoc1() {
            return assoc1;
        }

        public void setAssoc1(String assoc1) {
            this.assoc1 = assoc1;
        }

        public List<String> getAssocArr() {
            return assocArr;
        }

        public void setAssocArr(List<String> assocArr) {
            this.assocArr = assocArr;
        }
    }
}
