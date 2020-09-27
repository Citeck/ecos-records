package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsQueryDao;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnerSourceTest extends LocalRecordsDao implements LocalRecordsQueryDao {

    private static final String ID = "first";
    private static final String INNER_ID = "inner";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    @Test
    void test() {
        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID + "@" + INNER_ID);
        recordsService.query(query);
    }

    @NotNull
    @Override
    public RecordsQueryRes<RecordRef> queryLocalRecords(@NotNull RecordsQuery query) {
        assertEquals(INNER_ID, query.getSourceId());
        return new RecordsQueryRes<>();
    }
}
