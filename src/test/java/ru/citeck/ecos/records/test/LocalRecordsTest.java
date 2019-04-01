package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalRecordsTest {

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();
        recordsService.register(new RecordsQueryDAOImpl());
    }

    @Test
    void testSourceWithoutMetaInterface() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(RecordsQueryDAOImpl.ID);
        recordsService.queryRecords(query);
    }

    static class RecordsQueryDAOImpl extends LocalRecordsDAO implements RecordsQueryDAO {

        static String ID = "RecordsQueryDAOImpl";

        RecordsQueryDAOImpl() {
            setId(ID);
        }
    }
}
