package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RequestContextTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String SOURCE_ID = "test-source-id";

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        assertNull(RequestContext.getCurrent());

        recordsService.getAtts(Arrays.asList(
            RecordRef.create(SOURCE_ID, "1"),
            RecordRef.create(SOURCE_ID, "2"),
            RecordRef.create(SOURCE_ID, "3")
        ), TestMeta.class);

        assertNull(RequestContext.getCurrent());
    }

    public static class TestMeta {
        @AttName(".str")
        public String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {

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

        return records.stream().map(Record::new).collect(Collectors.toList());
    }

    public class Record implements AttValue {

        private RecordRef id;

        Record(String id) {
            this.id = RecordRef.valueOf(id);
            RequestContext.getCurrentNotNull().getList("testList").add(this);
        }

        @Override
        public String getId() {
            return id.toString();
        }

        @Override
        public String asText() {
            assertEquals(3, RequestContext.getCurrent().getList("testList").size());
            return "123";
        }
    }
}
