package ru.citeck.ecos.records2.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

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

        RecordsQuery query = RecordsQuery.create().withSourceId(RecordsQueryDaoImpl.ID).build();
        recordsService.query(query);
    }

    @Test
    void testWithMeta() {

        RecordsQuery query = RecordsQuery.create().withSourceId(RecordsWithMetaSource.ID).build();

        RecsQueryRes<EntityRef> result = recordsService.query(query);

        assertEquals(1, result.getRecords().size());
    }

    static class RecordsQueryDaoImpl implements RecordsQueryDao {

        static String ID = "RecordsQueryDaoImpl";

        @Nullable
        @Override
        public Object queryRecords(@NotNull RecordsQuery recsQuery) throws Exception {
            return null;
        }

        @NotNull
        @Override
        public String getId() {
            return ID;
        }
    }

    static class RecordsSource implements RecordAttsDao {

        static String ID = "recs-source";

        @Nullable
        @Override
        public Object getRecordAtts(@NotNull String recordId) throws Exception {
            return new Meta(recordId, recordId);
        }

        @NotNull
        @Override
        public String getId() {
            return ID;
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

    static class RecordsWithMetaSource implements RecordsQueryDao {

        static final String ID = "with-meta";

        @Nullable
        @Override
        public Object queryRecords(@NotNull RecordsQuery recsQuery) throws Exception {
            return List.of(new Meta());
        }

        @Override
        public String getId() {
            return ID;
        }

        static class Meta {}
    }
}
