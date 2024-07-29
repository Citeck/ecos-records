package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsConvertionTest extends AbstractRecordsDao implements RecordsQueryDao, SupportsQueryLanguages {

    private static final String ID = "test";

    private static final String SOURCE_LANG = "source";
    private static final String CONVERTED_LANG = "converted";

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        factory.getQueryLangService().register(query -> query, SOURCE_LANG, CONVERTED_LANG);

        recordsService.register(this);

        RecordsQuery query = RecordsQuery.create()
            .withQuery("123")
            .withLanguage(SOURCE_LANG)
            .withSourceId(ID)
            .build();

        recordsService.query(query);
    }

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        assertEquals(CONVERTED_LANG, query.getLanguage());
        return new RecsQueryRes<>();
    }

    @NotNull
    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList(CONVERTED_LANG);
    }
}

