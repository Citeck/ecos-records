package ru.citeck.ecos.records2.test.sourcedao;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsSourceRecordsDaoTest extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<Object> {

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
    public RecordsQueryResult<Object> queryLocalRecords(RecordsQuery query, MetaField field) {
        return new RecordsQueryResult<>();
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
        assertTrue(meta2.attsSupported);
        assertFalse(meta2.mutationSupported);
        assertFalse(meta2.querySupported);
    }

    @Data
    public static class SourceLangMetaInfo {
        private List<String> supportedLanguages;
    }

    @Data
    public static class SourceFlagsMetaInfo {
        private String id = "";
        @AttName("features.query")
        private Boolean querySupported;
        @AttName("features.getAtts")
        private Boolean attsSupported;
        @AttName("features.mutate")
        private Boolean mutationSupported;
        @AttName("features.delete")
        private Boolean deletionSupported;
    }
}
