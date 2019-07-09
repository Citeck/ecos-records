package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordRefValueFactoryTest extends LocalRecordsDAO
                                implements RecordsMetaLocalDAO<MetaValue>,
                                           RecordsQueryWithMetaLocalDAO<MetaValue> {

    private static final String FIELD_VALUE = "TestValue";
    private static final String FIELD_REF0 = "ref0Field";
    private static final String FIELD_REF1 = "ref1Field";
    private static final String REF1_DISP_NAME = "REF 1 DISP NAME";

    private static final String ID = "sourceId";

    private RecordsService recordsService;

    private RecordRef ref0 = RecordRef.create(ID, "ref0");
    private RecordRef ref1 = RecordRef.create(ID, "ref1");

    @BeforeAll
    void init() {
        setId(ID);

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();
        recordsService.register(this);
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        Map<String, String> attsToRequest = new HashMap<>();
        attsToRequest.put("att", FIELD_REF0 + "." + FIELD_REF1 + "?str");
        attsToRequest.put("disp", FIELD_REF0 + "?disp");

        RecordsQueryResult<RecordMeta> result = recordsService.queryRecords(query, attsToRequest);

        assertEquals(1, result.getRecords().size());
        RecordMeta meta = result.getRecords().get(0);
        assertEquals(FIELD_VALUE, meta.get("att", ""));
        assertEquals(REF1_DISP_NAME, meta.get("disp", ""));
    }

    @Override
    public RecordsQueryResult<MetaValue> getMetaValues(RecordsQuery query) {
        RecordsQueryResult<MetaValue> result = new RecordsQueryResult<>();
        result.addRecord(new Ref0());
        return result;
    }

    @Override
    public List<MetaValue> getMetaValues(List<RecordRef> records) {
        return records.stream().map(r -> {
            if (RecordRef.valueOf(ref0.getId()).equals(r)) {
                return new Ref0();
            } else if (RecordRef.valueOf(ref1.getId()).equals(r)) {
                return new Ref1();
            } else {
                throw new IllegalStateException("Unknown ref: " + r);
            }
        }).collect(Collectors.toList());
    }

    public class Ref0 implements MetaValue {

        @Override
        public String getId() {
            return ref0.getId();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            if (FIELD_REF0.equals(name)) {
                return ref1;
            }
            return null;
        }
    }

    public class Ref1 implements MetaValue {

        @Override
        public String getId() {
            return ref1.getId();
        }

        @Override
        public String getDisplayName() {
            return REF1_DISP_NAME;
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            if (FIELD_REF1.equals(name)) {
                return FIELD_VALUE;
            }
            return null;
        }
    }
}
