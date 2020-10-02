package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsConvertionTest extends AbstractRecordsDao implements RecordsQueryDao {

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

        recordsService.query(query);
    }

    @NotNull
    @Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        assertEquals(CONVERTED_LANG, query.getLanguage());
        return new RecordsQueryRes<>();
    }

    @NotNull
    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList(CONVERTED_LANG);
    }
}

