package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.dao.query.SupportsQueryLanguages;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;

import java.util.Collections;
import java.util.List;

public class SecurityUtilsTest implements RecordsQueryDao, SupportsQueryLanguages {

    private static final String ID = "test";

    @Test
    void test() {

        /*RecordsServiceFactory factory = new RecordsServiceFactory();
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
        }*/
    }

    @Override
    public RecsQueryRes<RecordAtts> queryRecords(@NotNull RecordsQuery query) {
        throw new RuntimeException("Exception while query");
    }

    @NotNull
    @Override
    public List<String> getSupportedLanguages() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }
}
