package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import java.util.*;

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
        result.setRecords(Arrays.asList("one", "two", "three"));

        assertEquals(10, result.getTotalCount());

        RecordsQueryResult<String> result2 = new RecordsQueryResult<>(result);

        assertEquals(10, result2.getTotalCount());

        result2.setRecords(Arrays.asList("one", "two", "three", "four", "five"));

        assertEquals(10, result2.getTotalCount());
    }

}
