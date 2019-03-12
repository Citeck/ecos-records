package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.lang.DistinctQuery;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.util.*;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordsGroupTest extends LocalRecordsDAO
                       implements RecordsQueryLocalDAO,
                                  RecordsQueryWithMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source";

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.createRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        return records.stream().map(r -> new PojoMeta(r.toString())).collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery recordsQuery) {

        RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();
        result.setRecords(query.getIds()
                               .stream()
                               .map(RecordRef::valueOf)
                               .collect(Collectors.toList()));

        result.setHasMore(false);
        result.setTotalCount(query.getIds().size());

        return result;
    }

    @Override
    public RecordsQueryResult<Object> getMetaValues(RecordsQuery recordsQuery) {

        if (recordsQuery.getLanguage().equals(DistinctQuery.LANGUAGE)) {

            DistinctQuery distinctQuery = recordsQuery.getQuery(DistinctQuery.class);

            RecordsQueryResult<Object> result = new RecordsQueryResult<>();
            result.setRecords(Arrays.asList("one", "two"));

            return result;

        } else {

            RecordsQueryResult<Object> result = new RecordsQueryResult<>();
            result.setRecords(Arrays.asList("record0", "record1"));



            return result;
        }



        return result;
    }

    @Test
    void SomeLongNametest() {
        System.out.println("EMPTY TEST");
    }

    public static class PojoMeta {

        private String id;
        private String fieldStr = "str_value";
        private Double number;

        public PojoMeta(String id) {

            this.id = id;
        }
    }
}
