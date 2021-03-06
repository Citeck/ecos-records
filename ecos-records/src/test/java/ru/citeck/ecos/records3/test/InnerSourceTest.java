package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnerSourceTest extends AbstractRecordsDao implements RecordsQueryDao {

    private static final String ID = "first";
    private static final String INNER_ID = "inner";

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);
    }

    @Test
    void test() {
        RecordsQuery query = RecordsQuery.create().withSourceId(ID + "@" + INNER_ID).build();
        recordsService.query(query);
    }

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        assertEquals(INNER_ID, query.getSourceId());
        return new RecsQueryRes<>();
    }
}
