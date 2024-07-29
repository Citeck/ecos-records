package ru.citeck.ecos.records2.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.api.promise.Promise;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryContextTest implements RecordAttsDao {

    private static final String SOURCE_ID = "test-source-id";

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        recordsService.register(this);
    }

    @Test
    void test() {

        assertNull(RequestContext.getCurrent());

        recordsService.getAtts(Arrays.asList(
            EntityRef.create(SOURCE_ID, "1"),
            EntityRef.create(SOURCE_ID, "2"),
            EntityRef.create(SOURCE_ID, "3")
        ), TestMeta.class);

        assertNull(RequestContext.getCurrent());
    }

    public static class TestMeta {
        @AttName("?str")
        public String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {

        RequestContext.getCurrent().incrementCount("test");
        RequestContext.getCurrent().incrementCount("test");
        RequestContext.getCurrent().incrementCount("test");
        RequestContext.getCurrent().incrementCount("test");

        assertEquals(4, RequestContext.getCurrent().getCount("test"));

        RequestContext.getCurrent().incrementCount("test2");
        RequestContext.getCurrent().incrementCount("test2");
        RequestContext.getCurrent().incrementCount("test2");

        assertEquals(4, RequestContext.getCurrent().getCount("test"));
        assertEquals(3, RequestContext.getCurrent().getCount("test2"));

        RequestContext.getCurrent().removeVar("test");
        RequestContext.getCurrent().removeVar("test2");

        return new Record(EntityRef.create(getId(), recordId));
    }

    @NotNull
    @Override
    public String getId() {
        return SOURCE_ID;
    }

    public class Record implements AttValue {

        private EntityRef id;

        Record(EntityRef id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id.toString();
        }

        @Nullable
        @Override
        public Promise<?> init() throws Exception {
            RequestContext.getCurrentNotNull().getList("testList").add(this);
            return null;
        }

        @Override
        public String asText() {
            assertEquals(3, RequestContext.getCurrentNotNull().getList("testList").size());
            return "123";
        }
    }
}
