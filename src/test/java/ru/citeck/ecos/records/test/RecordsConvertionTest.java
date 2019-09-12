package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsConvertionTest extends LocalRecordsDAO implements RecordsQueryLocalDAO {

    private static final String ID = "test";

    private static final String SOURCE_LANG = "source";
    private static final String CONVERTED_LANG = "converted";

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        factory.getQueryLangService().register(query -> query, SOURCE_LANG, CONVERTED_LANG);

        setId(ID);
        recordsService.register(this);

        RecordsQuery query = new RecordsQuery();
        query.setQuery("123");
        query.setLanguage(SOURCE_LANG);
        query.setSourceId(ID);

        recordsService.queryRecords(query);
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query) {
        assertEquals(CONVERTED_LANG, query.getLanguage());
        return new RecordsQueryResult<>();
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList(CONVERTED_LANG);
    }
}

