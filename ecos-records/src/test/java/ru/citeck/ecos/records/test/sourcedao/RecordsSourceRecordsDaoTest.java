package ru.citeck.ecos.records.test.sourcedao;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.annotation.AttName;
import ru.citeck.ecos.records3.record.operation.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsSourceRecordsDaoTest extends AbstractRecordsDao implements RecordsQueryDao {

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
    public RecordsQueryRes<Object> queryRecords(@NotNull RecordsQuery query) {
        return new RecordsQueryRes<>();
    }

    @NotNull
    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Test
    public void test() {
        SourceLangMetaInfo meta = recordsService.getAtts(RecordRef.create("source", "test"), SourceLangMetaInfo.class);
        assertEquals(SUPPORTED_LANGUAGES, meta.getSupportedLanguages());

        SourceFlagsMetaInfo meta2 = recordsService.getAtts(RecordRef.create("source", "meta"), SourceFlagsMetaInfo.class);
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
