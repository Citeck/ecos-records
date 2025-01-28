package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.request.RequestCtxData;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RequestContextTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String SOURCE_ID = "test-source-id";

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return SOURCE_ID;
    }

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);
    }

    @Test
    void srcIdMappingTest() {

        Map<String, String> sourceIdMapping0 = Collections.singletonMap("first", "second");
        Map<String, String> sourceIdMapping1 = Collections.singletonMap("third", "fourth");

        RequestContext.doWithCtxJ((b0) -> b0.withSourceIdMapping(sourceIdMapping0), (c0) -> {
            assertThat(c0.ctxData.getSourceIdMapping()).isEqualTo(sourceIdMapping0);
            RequestContext.doWithCtxJ((b1) -> b1.withSourceIdMapping(sourceIdMapping1), (c1) -> {
                Map<String, String> fullMapping = new HashMap<>(sourceIdMapping0);
                fullMapping.putAll(sourceIdMapping1);
                assertThat(c1.ctxData.getSourceIdMapping()).isEqualTo(fullMapping);
                return null;
            });
            assertThat(c0.ctxData.getSourceIdMapping()).isEqualTo(sourceIdMapping0);
            RequestContext.doWithCtxJ(RequestCtxData.Builder::withoutSourceIdMapping, (c1) -> {
                assertThat(c1.ctxData.getSourceIdMapping()).isEmpty();
                return null;
            });
            return null;
        });
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

        private EntityRef id;

        Record(String id) {
            this.id = EntityRef.valueOf(id);
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
