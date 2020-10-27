package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryExceptionTest extends AbstractRecordsDao implements RecordsAttsDao {

    private static final String MSG = "SomeMessage";

    private RecordsService recordsService;

    @NotNull
    @Override
    public String getId() {
        return "test";
    }

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsServiceV1();
        recordsService.register(this);
    }

    @Test
    void test() {
//todo
      /*  List<RecordRef> refs = Collections.singletonList(RecordRef.create("test", ""));
        RecordsResult<RecordAtts> res = recordsService.getAtts(refs, Collections.singletonList("str"));

        List<RecordsError> errors = res.getErrors();

        assertEquals(1, errors.size());
        RecordsError error = errors.get(0);

        assertEquals(MSG, error.getMsg());
        assertEquals(3, error.getStackTrace().size());
        assertTrue(error.getStackTrace().get(0).contains("QueryExceptionTest.java"));*/
    }

    @NotNull
    @Override
    public List<?> getRecordsAtts(@NotNull List<String> records) {
        try {
            try {
                throw new IllegalArgumentException(MSG);
            } catch (Exception e0) {
                throw new RuntimeException("runtime", e0);
            }
        } catch(Exception e1) {
            throw new IllegalStateException("state", e1);
        }
    }
}
