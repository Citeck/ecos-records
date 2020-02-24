package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.QueryConstants;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ForeachQueryTest extends LocalRecordsDAO implements RecordsQueryLocalDAO {

    private static final String ID = "";
    private RecordsService recordsService;

    private List<RecordsQuery> queries = new ArrayList<>();

    private List<DataValue> eachNode = Arrays.asList(
        new DataValue("firstText"),
        new DataValue("secondText"),
        new DataValue("thirdText")
    );

    private List<RecordRef> resultRefs = Arrays.asList(
        RecordRef.valueOf("first"),
        RecordRef.valueOf("second"),
        RecordRef.valueOf("third")
    );

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    private Object getQuery(String var) {
        return Json.getMapper().toJava(Json.getMapper().toJson(Predicates.and(
            Predicates.eq("test", var),
            Predicates.or(
                Predicates.eq("aaa", var),
                Predicates.contains("bbb", "123123"),
                Predicates.contains("ccc", var)
            ),
            Predicates.eq("test2", "test2")
        )));
    }

    @Test
    void test() {

        RecordsQuery query = new RecordsQuery();
        query.setQuery(getQuery(QueryConstants.IT_VAR));

        RecordsQueryResult<List<RecordRef>> result = recordsService.queryRecords(eachNode, query);
        List<List<RecordRef>> records = result.getRecords();

        assertTrue(result.getErrors().isEmpty());

        int idx = 0;
        for (List<RecordRef> qRecords : records) {

            assertEquals(1, qRecords.size());
            assertEquals(resultRefs.get(idx), qRecords.get(0));

            idx++;
        }

        assertEquals(3, idx);
        assertEquals(3, result.getTotalCount());
        assertFalse(result.getHasMore());

        assertEquals(3, queries.size());
        for (int i = 0; i < queries.size(); i++) {
            RecordsQuery resolvedQuery = queries.get(i);
            assertEquals(resolvedQuery.getQuery(), getQuery(eachNode.get(i).asText()));
        }
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query) {
        RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();
        result.setRecords(Collections.singletonList(resultRefs.get(queries.size())));
        queries.add(query);
        return result;
    }
}
