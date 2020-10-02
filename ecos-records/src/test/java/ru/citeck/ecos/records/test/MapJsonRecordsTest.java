package ru.citeck.ecos.records.test;

import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MapJsonRecordsTest extends AbstractRecordsDao
                                implements RecordsAttsDao {

    private static final String SOURCE_ID = "test-source-id";
    private static final RecordRef TEST_REF = RecordRef.create(SOURCE_ID, "TEST_REC_ID");

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        return records.stream().map(r -> {
            Map<String, Object> result = new HashMap<>();
            ObjectNode var = JsonNodeFactory.instance.objectNode();
            var.with("key1").with("key2").put("field", "Value0");
            result.put("key0", var);
            return result;
        }).collect(Collectors.toList());
    }

    @Test
    void testSingleStrAttribute() {

        DataValue value = recordsService.getAtt(TEST_REF,
            ".att(n:\"key0\"){att(n:\"key1\"){att(n:\"key2\"){att(n:\"field\"){str}}}}");

        assertEquals(DataValue.create("Value0"), value);

        value = recordsService.getAtt(TEST_REF,
            ".att(n:\"key1\"){att(n:\"key1\"){att(n:\"key2\"){att(n:\"field\"){str}}}}");

        assertEquals(DataValue.NULL, value);
    }
}
