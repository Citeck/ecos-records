package ru.citeck.ecos.records2.test;

import ecos.com.fasterxml.jackson210.databind.node.JsonNodeFactory;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MapJsonRecordsTest extends LocalRecordsDao
                                implements LocalRecordsMetaDao<Object> {

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
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
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

        DataValue value = recordsService.getAttribute(TEST_REF,
            ".att(n:\"key0\"){att(n:\"key1\"){att(n:\"key2\"){att(n:\"field\"){str}}}}");

        assertEquals(DataValue.create("Value0"), value);

        value = recordsService.getAttribute(TEST_REF,
            ".att(n:\"key1\"){att(n:\"key1\"){att(n:\"key2\"){att(n:\"field\"){str}}}}");

        assertEquals(DataValue.NULL, value);
    }
}
