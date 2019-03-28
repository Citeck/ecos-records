package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsMutationTest extends LocalRecordsDAO
                                implements MutableRecordsLocalDAO<RecordsMutationTest.TestDto> {

    private static final String SOURCE_ID = "test-source-id";
    private static final RecordRef TEST_REF = RecordRef.create(SOURCE_ID, "TEST_REC_ID");

    private RecordsService recordsService;

    private Map<RecordRef, TestDto> valuesToMutate;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);

        valuesToMutate = Collections.singletonMap(TEST_REF, new TestDto(TEST_REF));
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

        meta.set(".att(n:\"field\"){str}", "test__");
        meta.set(".att(n:\"field0\"){json}", "test0__");
        meta.set(".att(n:\"field1\"){disp}", "test1__");
        meta.set(".att(n:\"bool\"){bool}", false);
        mutation.setRecords(Collections.singletonList(meta));

        recordsService.mutate(mutation);

        assertEquals("test__", valuesToMutate.get(TEST_REF).getField());
        assertEquals("test0__", valuesToMutate.get(TEST_REF).getField0());
        assertEquals("test1__", valuesToMutate.get(TEST_REF).getField1());
        assertFalse(valuesToMutate.get(TEST_REF).isBool());
    }

    @Override
    public List<TestDto> getValuesToMutate(List<RecordRef> records) {
        return records.stream().map(r -> {
            TestDto testDto = valuesToMutate.get(r);
            if (testDto == null) {
                return Optional.<TestDto>empty();
            }
            return Optional.of(new TestDto(testDto));
        }).filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public RecordsMutResult save(List<TestDto> values) {

        RecordsMutResult result = new RecordsMutResult();
        List<RecordMeta> records = new ArrayList<>();

        values.forEach(v -> {
            TestDto testDto = valuesToMutate.get(v.getRef());
            if (testDto != null) {
                testDto.set(v);
                records.add(new RecordMeta(v.getRef()));
            }
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
    }
}
