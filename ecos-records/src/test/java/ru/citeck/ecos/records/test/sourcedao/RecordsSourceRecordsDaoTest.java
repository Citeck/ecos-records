package ru.citeck.ecos.records.test.sourcedao;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsQueryDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsSourceRecordsDaoTest extends LocalRecordsDao implements LocalRecordsQueryDao {

    private static final String ID = "test";
    private static final List<String> SUPPORTED_LANGUAGES = new ArrayList<>(Arrays.asList("one", "two", "three"));

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(ID);
        recordsService.register(this);
    }

    @Override
    public RecsQueryRes<Object> queryLocalRecords(RecordsQuery query) {
        return new RecsQueryRes<>();
    }

    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Test
    public void test() {
        SourceLangMetaInfo meta = recordsService.getMeta(RecordRef.create("source", "test"), SourceLangMetaInfo.class);
        assertEquals(SUPPORTED_LANGUAGES, meta.getSupportedLanguages());

        SourceFlagsMetaInfo meta2 = recordsService.getMeta(RecordRef.create("source", "meta"), SourceFlagsMetaInfo.class);
        assertEquals("meta", meta2.id);
        assertTrue(meta2.metaSupported);
        assertFalse(meta2.mutationSupported);
        assertFalse(meta2.queryWithMetaSupported);
        assertFalse(meta2.querySupported);
    }

    @Data
    public static class SourceLangMetaInfo {
        private List<String> supportedLanguages;
    }

    @Data
    public static class SourceFlagsMetaInfo {
        private String id = "";
        private Boolean queryWithMetaSupported;
        private Boolean querySupported;
        private Boolean metaSupported;
        private Boolean mutationSupported;
    }
}
