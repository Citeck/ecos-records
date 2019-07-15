package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnerSourceTest extends LocalRecordsDAO implements RecordsQueryLocalDAO {

    private static String ID = "first";
    private static String INNER_ID = "inner";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    @Test
    void test() {
        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID + "@" + INNER_ID);
        recordsService.queryRecords(query);
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query) {
        assertEquals(INNER_ID, query.getSourceId());
        return new RecordsQueryResult<>();
    }
}
