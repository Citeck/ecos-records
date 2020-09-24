package ru.citeck.ecos.records.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.record.op.query.QueryContext;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryContextTest extends LocalRecordsDao implements LocalRecordsMetaDao {

    private static final String SOURCE_ID = "test-source-id";

    private RecordsService recordsService;

    @BeforeAll
    void init() {

        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        setId(SOURCE_ID);
        recordsService.register(this);
    }

    @Test
    void test() {

        assertNull(QueryContext.getCurrent());

        recordsService.getMeta(Arrays.asList(
            RecordRef.create(SOURCE_ID, "1"),
            RecordRef.create(SOURCE_ID, "2"),
            RecordRef.create(SOURCE_ID, "3")
        ), TestMeta.class);

        assertNull(QueryContext.getCurrent());
    }

    public static class TestMeta {
        @MetaAtt(".str")
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
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records) {

        QueryContext.getCurrent().incrementCount("test");
        QueryContext.getCurrent().incrementCount("test");
        QueryContext.getCurrent().incrementCount("test");
        QueryContext.getCurrent().incrementCount("test");

        assertEquals(4, QueryContext.getCurrent().getCount("test"));

        QueryContext.getCurrent().incrementCount("test2");
        QueryContext.getCurrent().incrementCount("test2");
        QueryContext.getCurrent().incrementCount("test2");

        assertEquals(4, QueryContext.getCurrent().getCount("test"));
        assertEquals(3, QueryContext.getCurrent().getCount("test2"));

        return records.stream().map(Record::new).collect(Collectors.toList());
    }

    public class Record implements MetaValue {

        private RecordRef id;

        Record(RecordRef id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id.toString();
        }

        //todo
       /* @Override
        public <T extends QueryContext> void init(T context) {
            assertSame(QueryContext.getCurrent(), context);
            context.getList("testList").add(this);
        }*/

        @Override
        public String getString() {
            assertEquals(3, QueryContext.getCurrent().getList("testList").size());
            return "123";
        }
    }
}
