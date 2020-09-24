package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.error.RecordsError;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.request.result.RecordsResult;
import ru.citeck.ecos.records3.source.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.utils.SecurityUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecurityUtilsTest implements RecordsQueryDao {

    private static final String ID = "test";

    @Test
    void test() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        RecordsService service = factory.getRecordsService();
        service.register(this);

        RecordsQuery query = new RecordsQuery();

        query.setQuery("Test");
        query.setSourceId("Unknown");

        RecordsResult<RecordRef> result = service.queryRecords(query);
        result = SecurityUtils.encodeResult(result);

        for (RecordsError error : result.getErrors()) {
            String msg = error.getMsg();
            assertTrue(msg.contains("rcersdRQD") || msg.contains("rcersdRQWMD"), msg);
        }

        query.setSourceId(ID);

        result = service.queryRecords(query);
        result = SecurityUtils.encodeResult(result);

        List<String> notExpectedStrings = Arrays.asList("ru.citeck", "SecurityUtilsTest");

        for (RecordsError error : result.getErrors()) {
            String msg = error.getMsg();
            assertFalse(notExpectedStrings.stream().anyMatch(msg::contains));
            List<String> stackTrace = error.getStackTrace();
            if (stackTrace != null) {
                for (String traceLine : stackTrace) {
                    assertFalse(notExpectedStrings.stream()
                        .anyMatch(traceLine::contains), "Incorrect line: " + traceLine);
                }
            }
        }
    }

    @Override
    public RecsQueryRes<RecordMeta> queryRecords(@NotNull RecordsQuery query) {
        throw new RuntimeException("Exception while query");
    }

    @Override
    public String getId() {
        return ID;
    }
}
