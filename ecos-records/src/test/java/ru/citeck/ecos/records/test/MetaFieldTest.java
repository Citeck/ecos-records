package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetaFieldTest extends LocalRecordsDao
                           implements LocalRecordsMetaDao, MetaValue {

    private static final String SOURCE_ID = "test-source";
    private static final RecordRef RECORD_REF = RecordRef.create(SOURCE_ID, "test");

    private RecordsService recordsService;

    private boolean assertsPassed = false;

    @NotNull
    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records) {
        return Collections.singletonList(this);
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        Map<String, String> attributes = new HashMap<>();
        attributes.put("att", ".att(n:\"test\"){" +
                    "field0:att(n:\"one\") {" +
                        "innerOneAlias: att(n:\"innerOne\"){" +
                            "str" +
                        "}," +
                        "innerTwoAlias: atts(n:\"innerTwo\"){" +
                            "att(n:\"innerInnerTest\"){json}" +
                        "}," +
                        "disp" +
                    "}}");
        attributes.put("field1", "att(n:\"two\"){num}");

        assertsPassed = false;
        recordsService.getAttributes(Collections.singletonList(RECORD_REF), attributes);
        assertTrue(assertsPassed);
    }

    @Override
    public Object getAttribute(@NotNull String name) {

        //todo
       /* if ("field0".equals(field.getAlias())) {

            Map<String, String> expectedAttsMap = new HashMap<>();
            expectedAttsMap.put("innerOne", ".att(n:\"innerOne\"){str}");
            expectedAttsMap.put("innerTwo", ".atts(n:\"innerTwo\"){att(n:\"innerInnerTest\"){json}}");
            expectedAttsMap.put(".disp", ".disp");

            assertEquals(expectedAttsMap, new HashMap<>(field.getInnerAttributesMap()));

            List<String> expectedAttsList = new ArrayList<>();
            expectedAttsList.add("innerOne");
            expectedAttsList.add("innerTwo");
            expectedAttsList.add(".disp");

            assertEquals(expectedAttsList, new ArrayList<>(field.getInnerAttributes()));

            assertsPassed = true;
        }*/
        return this;
    }

    @Override
    public Double getDouble() {
        return 0.0;
    }

    @Override
    public Object getJson() {
        return "{}";
    }
}
