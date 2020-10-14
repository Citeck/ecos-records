package ru.citeck.ecos.records.test;import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.RecordsServiceImpl;

import ru.citeck.ecos.records3.record.op.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordsUtilsTest extends AbstractRecordsDao
    implements RecordsAttsDao {

    private static final String SOURCE_ID = "test-source";

    private RecordsServiceImpl recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = (RecordsServiceImpl) factory.getRecordsServiceV1();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        if (records.size() == 1 && records.get(0).isEmpty()) {
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
