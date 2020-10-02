package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.operation.query.QueryConstants;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.predicate.model.Predicates;
import ru.citeck.ecos.records3.record.operation.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ForeachQueryTest extends AbstractRecordsDao implements RecordsQueryDao {

    private static final String ID = "";
    private RecordsService recordsService;

    private final List<RecordsQuery> queries = new ArrayList<>();

    private final List<DataValue> eachNode = Arrays.asList(
        DataValue.create("firstText"),
        DataValue.create("secondText"),
        DataValue.create("thirdText")
    );

    private final List<RecordRef> resultRefs = Arrays.asList(
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

        RecordsQueryRes<List<RecordRef>> result = recordsService.query(eachNode, query);
        List<List<RecordRef>> records = result.getRecords();

        //assertTrue(result.getErrors().isEmpty());

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

    @NotNull
    @Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        RecordsQueryRes<RecordRef> result = new RecordsQueryRes<>();
        result.setRecords(Collections.singletonList(resultRefs.get(queries.size())));
        queries.add(query);
        return result;
    }
}
