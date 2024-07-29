package ru.citeck.ecos.records2.test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityRefValueFactoryTest implements RecordAttsDao, RecordsQueryDao {

    private static final String ID = "sourceId";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(ID)
            .build();

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
        RecordAtts meta = result.getRecords().getFirst();
        assertEquals(Val.val0.value, meta.getAtts().get("att0", ""));
        assertEquals(Val.val2.value, meta.getAtts().get("att2", ""));
        assertEquals(Val.val1.getDisplayName(), meta.getAtts().get("disp", ""));
        assertEquals(Val.val0.ref.toString(), meta.getAtts().get("assoc", ""));

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
        assertEquals(Val.globalRef.getLocalId(), id);
    }

    @Test
    void test2() {

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(ID)
            .build();

        Map<String, String> attsToRequest = new HashMap<>();
        attsToRequest.put("null_0", Val.VAL0_FIELD + "." + Val.VAL1_FIELD + ".unknown?str");

        RecsQueryRes<RecordAtts> result = recordsService.query(query, attsToRequest);

        assertEquals(1, result.getRecords().size());
        RecordAtts meta = result.getRecords().getFirst();

        assertEquals(DataValue.NULL, meta.get("null_0"));
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        if (recordId.equals(Val.val0.getId())) {
            return Val.val0;
        } else if (recordId.equals(Val.val1.getId())) {
            return Val.val1;
        } else if (recordId.equals(Val.val2.getId())) {
            return Val.val2;
        } else {
            throw new IllegalStateException("Unknown ref: " + recordId);
        }
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recsQuery) throws Exception {
        return List.of(Val.val0);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    public static class Val implements AttValue {

        static final String VALUE_FIELD = "value";
        static final String VAL0_FIELD = "val0";
        static final String VAL1_FIELD = "val1";
        static final String VAL2_FIELD = "val2";
        static final String ARR_VALUE_FIELD = "value_arr";

        static final Val val0 = new Val(EntityRef.create(ID, "val0"));
        static final Val val1 = new Val(EntityRef.create(ID, "val1"));
        static final Val val2 = new Val(EntityRef.create(ID, "val2"));
        static final EntityRef globalRef = EntityRef.create("global", "local", "ref");

        final EntityRef ref;
        final String value;

        public Val(EntityRef ref) {
            this.ref = ref;
            this.value = "VALUE OF " + ref;
        }

        @Override
        public String getId() {
            return ref.getLocalId();
        }

        @Override
        public String getDisplayName() {
            return "DISP OF " + ref;
        }

        @Override
        public String asText() {
            return "STR OF " + ref;
        }

        @Override
        public boolean has(String name) {
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
        public Object getAtt(String name) {
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
