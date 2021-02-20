package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;

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
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(new RecordsQueryDaoImpl());
        recordsService.register(new RecordsSource());
        recordsService.register(new RecordsWithMetaSource());
    }

    @Test
    void testSourceWithoutMetaInterface() {

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(RecordsQueryDaoImpl.ID)
            .build();

        recordsService.query(query);
    }

    @Test
    void testRecordWithComplexSourceId() {

        RecordRef ref = RecordRef.valueOf(RecordsSource.ID + "@first@second");
        RecordAtts meta = recordsService.getAtts(ref, Collections.singleton("localId"));

        assertEquals(ref, meta.getId());
    }

    @Test
    void testWithMeta() {

        RecordsQuery query = RecordsQuery.create()
            .withSourceId(RecordsWithMetaSource.ID)
            .build();

        RecsQueryRes<RecordRef> result = recordsService.query(query);

        assertEquals(1, result.getRecords().size());
    }

    static class RecordsQueryDaoImpl extends AbstractRecordsDao implements RecordsQueryDao {

        static String ID = "RecordsQueryDaoImpl";

        @NotNull
        @Override
        public String getId() {
            return ID;
        }

        @Nullable
        @Override
        public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
            return null;
        }
    }

    static class RecordsSource extends AbstractRecordsDao implements RecordsAttsDao {

        static String ID = "recs-source";

        @NotNull
        @Override
        public String getId() {
            return ID;
        }

        @NotNull
        @Override
        public List<Meta> getRecordsAtts(@NotNull List<String> records) {
            return records.stream()
                .map(r -> new Meta(r, r))
                .collect(Collectors.toList());
        }

        public static class Meta {

            private final String id;
            private final String localId;
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

    static class RecordsWithMetaSource extends AbstractRecordsDao
        implements RecordsQueryDao {

        static final String ID = "with-meta";

        @NotNull
        @Override
        public RecsQueryRes<Meta> queryRecords(@NotNull RecordsQuery query) {
            RecsQueryRes<Meta> result = new RecsQueryRes<>();
            result.addRecord(new Meta());
            return result;
        }

        @NotNull
        @Override
        public String getId() {
            return ID;
        }

        static class Meta {}
    }
}
