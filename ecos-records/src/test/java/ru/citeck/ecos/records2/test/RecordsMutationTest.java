package ru.citeck.ecos.records2.test;

import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records3.RecordsServiceFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsMutationTest extends LocalRecordsDao
                                implements MutableRecordsLocalDao<RecordsMutationTest.TestDto> {

    private static final String SOURCE_ID = "test-source-id";
    private static final RecordRef TEST_REF = RecordRef.create(SOURCE_ID, "TEST_REC_ID");

    private static final String ALIAS_VALUE = "ALIAS";

    private RecordsService recordsService;

    private Map<RecordRef, TestDto> valuesToMutate;
    private Map<RecordRef, TestDto> newValues;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);

        valuesToMutate = Collections.singletonMap(TEST_REF, new TestDto(TEST_REF));
        newValues = new HashMap<>();
    }

    @Test
    void test() {

        RecordsMutation mutation = new RecordsMutation();

        RecordMeta meta = new RecordMeta(TEST_REF);
        meta.set("field", "test");
        meta.set("field0", "test0");
        meta.set("field1", "test1");
        meta.set("bool", true);
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);

        assertEquals("test", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1", valuesToMutate.get(TEST_REF).getField1());
        assertTrue(valuesToMutate.get(TEST_REF).isBool());

        meta = new RecordMeta(TEST_REF);
        meta.set("field{.str}", "__test__");
        meta.set("field0{.json}", "__test0__");
        meta.set("field1{.disp}", "__test1__");
        meta.set("bool{.bool}", false);
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);

        assertEquals("__test__", valuesToMutate.get(TEST_REF).getField());
        assertEquals("__test0__", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("__test1__", valuesToMutate.get(TEST_REF).getField1());
        assertFalse(valuesToMutate.get(TEST_REF).isBool());

        meta = new RecordMeta(TEST_REF);
        meta.set(".att(n:\"field\"){str}", "test__");
        meta.set(".att(n:\"field0\"){json}", "test0__");
        meta.set(".att(n:\"field1\"){disp}", "test1__");
        meta.set(".att(n:\"bool\"){bool}", true);
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);

        assertEquals("test__", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0__", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1__", valuesToMutate.get(TEST_REF).getField1());
        assertTrue(valuesToMutate.get(TEST_REF).isBool());

        meta = new RecordMeta(TEST_REF);
        meta.set(".att(\"field\"){str}", "test____");
        meta.set(".att(\"field0\"){json}", "test0____");
        meta.set(".att('field1'){disp}", "test1____");
        meta.set(".att('bool'){bool}", false);
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);

        assertEquals("test____", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0____", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1____", valuesToMutate.get(TEST_REF).getField1());
        assertFalse(valuesToMutate.get(TEST_REF).isBool());

        assertEquals(0, newValues.size());

        mutation = new RecordsMutation();

        RecordRef newRef = RecordRef.create(SOURCE_ID, "newRef");
        meta = new RecordMeta(newRef);
        meta.set("field", "test");
        meta.set("field0", "test0");
        meta.set("field1", "test1");
        meta.set("bool", false);
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);

        assertEquals(1, newValues.size());

        TestDto newDto = newValues.values().stream().findFirst().get();
        assertEquals(newRef.toString() + "-new", newDto.getRef().toString());
        assertEquals("test", newDto.getField());
        assertEquals("test0", newDto.getField0());
        assertEquals("test1", newDto.getField1());
        assertFalse(newDto.isBool());

        newValues.clear();

        RecordRef withInnerRef = RecordRef.create(SOURCE_ID, "newRefWithInner");
        RecordMeta newWithInner = new RecordMeta(withInnerRef);
        newWithInner.set("field", "value123");

        RecordRef innerRef = RecordRef.create(SOURCE_ID, "newInner");

        RecordMeta newInner = new RecordMeta(innerRef);
        newInner.set(RecordConstants.ATT_ALIAS, ALIAS_VALUE);
        newWithInner.set(".att(n:\"assoc0\"){assoc}", ALIAS_VALUE);
        newWithInner.set(".atts(n:\"assoc1\"){assoc}", ALIAS_VALUE);
        ArrayNode innerArrayNode = JsonNodeFactory.instance.arrayNode();
        innerArrayNode.add(ALIAS_VALUE);
        newWithInner.set("assocArr?assoc", innerArrayNode);

        mutation = new RecordsMutation();
        mutation.setRecords(Arrays.asList(newWithInner, newInner));

        recordsService.mutate(mutation);

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
    protected RecordsMutResult mutateImpl(RecordsMutation mutation) {

        for (RecordMeta meta : mutation.getRecords()) {
            meta.forEachJ((name, value) -> {
                assertFalse(name.startsWith("."));
                assertFalse(name.contains("?"));
                if (!RecordConstants.ATT_ALIAS.equals(name)) {
                    assertNotEquals(ALIAS_VALUE, value.asText());
                }
            });
        }

        return super.mutateImpl(mutation);
    }

    @NotNull
    @Override
    public List<TestDto> getValuesToMutate(@NotNull List<RecordRef> records) {
        return records.stream().map(r -> {
            RecordRef ref = RecordRef.create(getId(), r);
            TestDto testDto = valuesToMutate.get(ref);
            return testDto == null ? new TestDto(RecordRef.valueOf(ref + "-new")) : new TestDto(testDto);
        }).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public RecordsMutResult save(@NotNull List<TestDto> values) {

        RecordsMutResult result = new RecordsMutResult();
        List<RecordMeta> records = new ArrayList<>();

        values.forEach(v -> {
            TestDto testDto = valuesToMutate.get(v.getRef());
            if (testDto != null) {
                testDto.set(v);
            } else {
                newValues.put(v.getRef(), v);
            }
            records.add(new RecordMeta(v.getRef().getId()));
        });

        result.setRecords(records);
        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        return null;
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
