package ru.citeck.ecos.records2.test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MapJsonRecordsTest implements RecordAttsDao {

    private static final String SOURCE_ID = "test-source-id";
    private static final EntityRef TEST_REF = EntityRef.create(SOURCE_ID, "TEST_REC_ID");

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        recordsService.register(this);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        Map<String, Object> result = new HashMap<>();
        ObjectNode var = JsonNodeFactory.instance.objectNode();
        var.with("key1").with("key2").put("field", "Value0");
        result.put("key0", var);
        return result;
    }

    @NotNull
    @Override
    public String getId() {
        return SOURCE_ID;
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
