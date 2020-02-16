package ru.citeck.ecos.records.test.scalar;

import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.scalar.MLText;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MLTextTest extends LocalRecordsDAO implements LocalRecordsQueryWithMetaDAO<Object> {

    private static final String ID = "test";

    private static final String EN_VALUE = "EN VALUE";
    private static final String RU_VALUE = "RU VALUE";

    private static final MLText testML = new MLText();

    static {
        testML.put(Locale.ENGLISH, EN_VALUE);
        testML.put(new Locale("ru"), RU_VALUE);
    }

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        setId(ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        String enValue = "EN_VALUE";
        String ruValue = "RU_VALUE";

        String mlAsObject = "{\"en\": \"" + enValue + "\", \"ru\":\"" + ruValue + "\"}";

        String withInnerAsObj = "{\"textField\": " + mlAsObject + "}";
        String withInnerAsStr = "{\"textField\": \"" + enValue + "\"}";

        WithInnerField strTxt0 = JsonUtils.read(withInnerAsStr, WithInnerField.class);
        WithInnerField objTxt0 = JsonUtils.read(withInnerAsObj, WithInnerField.class);

        assertEquals(strTxt0.textField.get(Locale.ENGLISH), objTxt0.textField.get(Locale.ENGLISH));

        assertNotEquals(strTxt0, objTxt0);
        strTxt0.textField.put(new Locale("ru"), ruValue);
        assertEquals(strTxt0, objTxt0);

        String withInnerFromObj = JsonUtils.toString(objTxt0);
        WithInnerField withInnerObjStrObj = JsonUtils.read(withInnerFromObj, WithInnerField.class);

        assertEquals(objTxt0, withInnerObjStrObj);

        String testValue = "TestValue";
        MLText mlText = JsonUtils.read("{\"ru\":\"" + testValue + "\"}", MLText.class);
        assertEquals(testValue, mlText.getClosestValue(Locale.ENGLISH));
        assertNull(mlText.get(Locale.ENGLISH));
        assertEquals(testValue, mlText.get(new Locale("ru")));
    }

    @Test
    void testWithRecords() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(ID);

        List<String> atts = new ArrayList<>();
        atts.add("ru");
        atts.add(Locale.ENGLISH.toString());
        atts.add(".str");
        atts.add("eu");
        atts.add(".json");

        RecordMeta res = recordsService.queryRecords(query, atts).getRecords().get(0);
        assertEquals(EN_VALUE, res.get(Locale.ENGLISH.toString(), ""));
        assertEquals(RU_VALUE, res.get("ru", ""));
        assertEquals(EN_VALUE, res.get(".str", ""));
        assertEquals(EN_VALUE, res.get("eu", ""));

        assertEquals(testML, JsonUtils.convert(res.get(".json"), MLText.class));
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(RecordsQuery query, MetaField field) {
        return RecordsQueryResult.of(testML);
    }

    @Data
    public static class WithInnerField {
        private MLText textField;
    }
}
