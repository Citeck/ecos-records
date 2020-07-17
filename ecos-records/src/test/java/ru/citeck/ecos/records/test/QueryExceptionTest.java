package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryExceptionTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

    private static final String MSG = "SomeMessage";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId("test");
        recordsService.register(this);
    }

    @Test
    void test() {

        List<RecordRef> refs = Collections.singletonList(RecordRef.create("test", ""));
        RecordsResult<RecordMeta> res = recordsService.getMeta(refs, "str");

        List<RecordsError> errors = res.getErrors();

        assertEquals(1, errors.size());
        RecordsError error = errors.get(0);

        assertEquals(MSG, error.getMsg());
        assertEquals(3, error.getStackTrace().size());
        assertTrue(error.getStackTrace().get(0).contains("QueryExceptionTest.java"));
    }

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        try {
            try {
                throw new IllegalArgumentException(MSG);
            } catch (Exception e0) {
                throw new RuntimeException("runtime", e0);
            }
        } catch(Exception e1) {
            throw new IllegalStateException("state", e1);
        }
    }
}
