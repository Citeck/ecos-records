package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.RecordsServiceImpl;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordsUtilsTest extends LocalRecordsDAO
    implements RecordsMetaLocalDAO<Object> {

    private static final String SOURCE_ID = "test-source";

    private RecordsServiceImpl recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = (RecordsServiceImpl) factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Override
    public List<Object> getMetaValues(List<RecordRef> records) {
        if (records.size() == 1 && records.get(0) == RecordRef.EMPTY) {
            return Collections.singletonList(new TestDto());
        }
        return null;
    }

    @Test
    void test() {

        List<String> attributes = Arrays.asList("strField", "doubleField", "unknown");

        Map<String, Class<?>> attributesClasses = RecordsUtils.getAttributesClasses(SOURCE_ID,
                                                                                    attributes,
                                                                                    Object.class,
                                                                                    recordsService);

        assertEquals(3, attributesClasses.size());
        assertEquals(String.class, attributesClasses.get("strField"));
        assertEquals(Double.class, attributesClasses.get("doubleField"));
        assertEquals(Object.class, attributesClasses.get("unknown"));

        attributesClasses = RecordsUtils.getAttributesClasses(SOURCE_ID,
                                                              attributes,
                                                              null,
                                                              recordsService);
        assertEquals(2, attributesClasses.size());
        assertEquals(String.class, attributesClasses.get("strField"));
        assertEquals(Double.class, attributesClasses.get("doubleField"));
        assertNull(attributesClasses.get("unknown"));
    }

    public static final class TestDto {

        private String strField;
        private Double doubleField;

        public String getStrField() {
            return strField;
        }

        public void setStrField(String strField) {
            this.strField = strField;
        }

        public Double getDoubleField() {
            return doubleField;
        }

        public void setDoubleField(Double doubleField) {
            this.doubleField = doubleField;
        }
    }
}
