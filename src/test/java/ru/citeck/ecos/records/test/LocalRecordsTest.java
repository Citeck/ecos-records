package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalRecordsTest {

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();
        recordsService.register(new RecordsQueryDAOImpl());
        recordsService.register(new RecordsSource());
        recordsService.register(new RecordsWithMetaSource());
    }

    @Test
    void testSourceWithoutMetaInterface() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(RecordsQueryDAOImpl.ID);
        recordsService.queryRecords(query);
    }

    @Test
    void testRecordWithComplexSourceId() {

        RecordRef ref = RecordRef.valueOf(RecordsSource.ID + "@first@second");
        RecordMeta meta = recordsService.getAttributes(ref, Collections.singleton("localId"));

        assertEquals(ref, meta.getId());
    }

    @Test
    void testWithMeta() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(RecordsWithMetaSource.ID);

        RecordsQueryResult<RecordRef> result = recordsService.queryRecords(query);

        assertEquals(1, result.getRecords().size());
    }

    static class RecordsQueryDAOImpl extends LocalRecordsDAO implements RecordsQueryDAO {

        static String ID = "RecordsQueryDAOImpl";

        RecordsQueryDAOImpl() {
            setId(ID);
        }
    }

    static class RecordsSource extends LocalRecordsDAO implements RecordsMetaLocalDAO<RecordsSource.Meta> {

        static String ID = "recs-source";

        RecordsSource() {
            setId(ID);
        }

        @Override
        public List<Meta> getMetaValues(List<RecordRef> records) {
            return records.stream().map(r -> new Meta(r.toString(), r.getId())).collect(Collectors.toList());
        }

        public static class Meta {

            private final String id;
            private String localId;
            private String field1;

            Meta(String id, String localId) {
                this.id = id;
                this.localId = localId;
            }

            public String getId() {
                return id;
            }

            public String getLocalId() {
                return localId;
            }

            public String getField1() {
                return field1;
            }

            public void setField1(String field1) {
                this.field1 = field1;
            }
        }
    }

    static class RecordsWithMetaSource extends LocalRecordsDAO
        implements RecordsQueryWithMetaLocalDAO<RecordsWithMetaSource.Meta> {

        static final String ID = "with-meta";

        RecordsWithMetaSource() {
            setId(ID);
        }

        @Override
        public RecordsQueryResult<Meta> getMetaValues(RecordsQuery query) {
            RecordsQueryResult<Meta> result = new RecordsQueryResult<>();
            result.addRecord(new Meta());
            return result;
        }

        @Override
        public String getId() {
            return ID;
        }

        static class Meta {}
    }
}
