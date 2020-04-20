package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class RecordsQueryResultTest {

    @Test
    void testTotalCount() {

        RecordsQueryResult<String> result = new RecordsQueryResult<>();
        result.setRecords(Arrays.asList("one", "two", "three"));

        assertEquals(3, result.getTotalCount());

        result.addRecord("four");

        assertEquals(4, result.getTotalCount());

        result.setTotalCount(10);
        assertEquals(10, result.getTotalCount());
        result.setRecords(Arrays.asList("one", "two", "three"));
        assertEquals(10, result.getTotalCount());

        RecordsQueryResult<String> result2 = new RecordsQueryResult<>(result);

        assertEquals(10, result2.getTotalCount());
        result2.setRecords(Arrays.asList("one", "two", "three", "four", "five"));
        assertEquals(10, result2.getTotalCount());

        result2.setRecords(IntStream.range(0, 20)
                                    .mapToObj(String::valueOf)
                                    .collect(Collectors.toList()));

        assertEquals(20, result2.getTotalCount());
        assertEquals(10, result.getTotalCount());
        assertEquals(3, result.getRecords().size());
    }

    @Test
    void constructorTest() {

        RecordsQueryResult<Object> qRes0 = new RecordsQueryResult<>();
        qRes0.setErrors(Collections.singletonList(new RecordsError("Test")));

        RecordsQueryResult<Object> qRes1 = new RecordsQueryResult<>(qRes0);
        assertEquals(1, qRes1.getErrors().size());
        assertEquals("Test", qRes1.getErrors().get(0).getMsg());

        qRes1.addError(new RecordsError("Test2"));
        assertEquals(1, qRes0.getErrors().size());
    }

    @Test
    void debugTest() {
        RecordsQueryResult<Object> res = new RecordsQueryResult<>();
        res.setDebug(null);
        assertEquals(new ObjectData(), res.getDebug());
    }
}
