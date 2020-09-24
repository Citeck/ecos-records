package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsQueryDao;

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
        recordsService = factory.getRecordsService();
        recordsService.register(new RecordsQueryDaoImpl());
        recordsService.register(new RecordsSource());
        recordsService.register(new RecordsWithMetaSource());
    }

    @Test
    void testSourceWithoutMetaInterface() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(RecordsQueryDaoImpl.ID);
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

        RecsQueryRes<RecordRef> result = recordsService.queryRecords(query);

        assertEquals(1, result.getRecords().size());
    }

    static class RecordsQueryDaoImpl extends LocalRecordsDao implements RecordsQueryDao {

        static String ID = "RecordsQueryDaoImpl";

        RecordsQueryDaoImpl() {
            setId(ID);
        }
    }

    static class RecordsSource extends LocalRecordsDao implements LocalRecordsMetaDao {

        static String ID = "recs-source";

        RecordsSource() {
            setId(ID);
        }

        @NotNull
        @Override
        public List<Meta> getLocalRecordsMeta(@NotNull List<RecordRef> records) {
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

    static class RecordsWithMetaSource extends LocalRecordsDao
        implements LocalRecordsQueryDao {

        static final String ID = "with-meta";

        RecordsWithMetaSource() {
            setId(ID);
        }

        @NotNull
        @Override
        public RecsQueryRes<Meta> queryLocalRecords(@NotNull RecordsQuery query) {
            RecsQueryRes<Meta> result = new RecsQueryRes<>();
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
