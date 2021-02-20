package ru.citeck.ecos.records3.test;

import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class RecordsQueryResTest {

    @Test
    void testTotalCount() {

        RecsQueryRes<String> result = new RecsQueryRes<>();
        result.setRecords(Arrays.asList("one", "two", "three"));

        assertEquals(3, result.getTotalCount());

        result.addRecord("four");

        assertEquals(4, result.getTotalCount());

        result.setTotalCount(10);
        assertEquals(10, result.getTotalCount());
        result.setRecords(Arrays.asList("one", "two", "three"));
        assertEquals(10, result.getTotalCount());

        RecsQueryRes<String> result2 = new RecsQueryRes<>(result);

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

        /*RecordsQueryRes<Object> qRes0 = new RecordsQueryRes<>();
        qRes0.setErrors(Collections.singletonList(new RecordsError("Test")));

        RecordsQueryRes<Object> qRes1 = new RecordsQueryRes<>(qRes0);
        assertEquals(1, qRes1.getErrors().size());
        assertEquals("Test", qRes1.getErrors().get(0).getMsg());

        qRes1.addError(new RecordsError("Test2"));
        assertEquals(1, qRes0.getErrors().size());*/
    }

    @Test
    void debugTest() {
        /*RecordsQueryRes<Object> res = new RecordsQueryRes<>();
        res.setDebug(null);
        assertEquals(ObjectData.create(), res.getDebug());*/
    }
}
