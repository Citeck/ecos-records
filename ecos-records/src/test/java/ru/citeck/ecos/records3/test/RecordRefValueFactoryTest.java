package ru.citeck.ecos.records3.test;

import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordRefValueFactoryTest extends AbstractRecordsDao
                                implements RecordsAttsDao,
                                           RecordsQueryDao {

    private static final String ID = "sourceId";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        setId(ID);

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);
    }

    @Test
    void testt() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        Map<String, String> attsToRequest = new HashMap<>();
        attsToRequest.put("as_with_alias_has_true", ".att(n:\"" + Val.VAL0_FIELD + "\"){att(n:\"" + Val.VAL1_FIELD + "\"){alias_for_as:as(n:\"abc\"){has(n:\"has_true\")}}}");

        RecsQueryRes<RecordAtts> result = recordsService.query(query, attsToRequest);

        RecordAtts meta = result.getRecords().get(0);
        assertEquals(DataValue.TRUE, meta.get("as_with_alias_has_true"));
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        Map<String, String> attsToRequest = new HashMap<>();
        attsToRequest.put("att0", Val.VAL0_FIELD + "." + Val.VAL0_FIELD + "." + Val.VAL0_FIELD + "." + Val.VALUE_FIELD + "?str");
        attsToRequest.put("att2", Val.VAL0_FIELD + "." + Val.VAL1_FIELD + "." + Val.VAL2_FIELD + "." + Val.VALUE_FIELD + "?str");
        attsToRequest.put("att3", Val.VAL0_FIELD + "." + Val.VAL1_FIELD + "." + Val.VAL2_FIELD + "." + Val.ARR_VALUE_FIELD + "[]?str");

        attsToRequest.put("has_true", ".att(n:\"" + Val.VAL0_FIELD + "\"){att(n:\"" + Val.VAL1_FIELD + "\"){has(n:\"has_true\")}}");
        attsToRequest.put("has_false", ".att(n:\"" + Val.VAL0_FIELD + "\"){att(n:\"" + Val.VAL1_FIELD + "\"){has(n:\"has_false\")}}");

        attsToRequest.put("as_has_true", ".att(n:\"" + Val.VAL0_FIELD + "\"){att(n:\"" + Val.VAL1_FIELD + "\"){as(n:\"abc\"){has(n:\"has_true\")}}}");
        attsToRequest.put("as_has_false", ".att(n:\"" + Val.VAL0_FIELD + "\"){att(n:\"" + Val.VAL1_FIELD + "\"){as(n:\"abc\"){has(n:\"has_false\")}}}");

        attsToRequest.put("as_with_alias_has_true", ".att(n:\"" + Val.VAL0_FIELD + "\"){att(n:\"" + Val.VAL1_FIELD + "\"){alias_for_as:as(n:\"abc\"){has(n:\"has_true\")}}}");

        attsToRequest.put("disp", Val.VAL1_FIELD + "?disp");
        attsToRequest.put("assoc", Val.VAL0_FIELD + "?assoc");

        RecsQueryRes<RecordAtts> result = recordsService.query(query, attsToRequest);

        assertEquals(1, result.getRecords().size());
        RecordAtts meta = result.getRecords().get(0);
        assertEquals(Val.val0.value, meta.get("att0", ""));
        assertEquals(Val.val2.value, meta.get("att2", ""));
        assertEquals(Val.val1.getDisplayName(), meta.get("disp", ""));
        assertEquals(Val.val0.ref.toString(), meta.get("assoc", ""));

        assertEquals(DataValue.TRUE, meta.get("has_true"));
        assertEquals(DataValue.FALSE, meta.get("has_false"));

        assertEquals(DataValue.TRUE, meta.get("as_has_true"));
        assertEquals(DataValue.FALSE, meta.get("as_has_false"));

        assertEquals(DataValue.TRUE, meta.get("as_with_alias_has_true"));

        ArrayNode expected = JsonNodeFactory.instance.arrayNode();
        expected.add(Val.val2.value);
        expected.add(Val.val2.value);

        assertEquals(DataValue.create(expected), meta.get("att3"));
    }

    @Test
    void test3() {

        String id = recordsService.getAtt(Val.val0.ref, "globalRef?localId").asText();
        assertEquals(Val.globalRef.getId(), id);
    }

    @Test
    void test2() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        Map<String, String> attsToRequest = new HashMap<>();
        attsToRequest.put("null_0", Val.VAL0_FIELD + "." + Val.VAL1_FIELD + ".unknown?str");

        RecsQueryRes<RecordAtts> result = recordsService.query(query, attsToRequest);

        assertEquals(1, result.getRecords().size());
        RecordAtts meta = result.getRecords().get(0);

        assertEquals(DataValue.NULL, meta.get("null_0"));
    }

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        RecsQueryRes<AttValue> result = new RecsQueryRes<>();
        result.addRecord(Val.val0);
        return result;
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream().map(r -> {
            if (r.equals(Val.val0.getId())) {
                return Val.val0;
            } else if (r.equals(Val.val1.getId())) {
                return Val.val1;
            } else if (r.equals(Val.val2.getId())) {
                return Val.val2;
            } else {
                throw new IllegalStateException("Unknown ref: " + r);
            }
        }).collect(Collectors.toList());
    }

    public static class Val implements AttValue {

        static final String VALUE_FIELD = "value";
        static final String VAL0_FIELD = "val0";
        static final String VAL1_FIELD = "val1";
        static final String VAL2_FIELD = "val2";
        static final String ARR_VALUE_FIELD = "value_arr";

        static final Val val0 = new Val(RecordRef.create(ID, "val0"));
        static final Val val1 = new Val(RecordRef.create(ID, "val1"));
        static final Val val2 = new Val(RecordRef.create(ID, "val2"));
        static final RecordRef globalRef = RecordRef.create("global", "local", "ref");

        final RecordRef ref;
        final String value;

        public Val(RecordRef ref) {
            this.ref = ref;
            this.value = "VALUE OF " + ref;
        }

        @Override
        public String getId() {
            return ref.getId();
        }

        @Override
        public String getDisplayName() {
            return "DISP OF " + ref;
        }

        @Override
        public String getString() {
            return "STR OF " + ref;
        }

        @Override
        public boolean has(@NotNull String name) {
            if (name.equals("has_true")) {
                return true;
            } else if (name.equals("has_false")) {
                return false;
            }
            throw new IllegalArgumentException("Unknown name: " + name);
        }

        @Override
        public Object getAs(@NotNull String name) {
            return this;
        }

        @Override
        public Object getAtt(@NotNull String name) {
            switch (name) {
                case VAL0_FIELD:
                    return val0.ref;
                case VAL1_FIELD:
                    return val1.ref;
                case VAL2_FIELD:
                    return val2.ref;
                case VALUE_FIELD:
                    return value;
                case ARR_VALUE_FIELD:
                    return Arrays.asList(value, value);
                case "globalRef":
                    return globalRef;
                default:
                    return null;
            }
        }
    }
}
