package ru.citeck.ecos.records2.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryExceptionTest implements RecordAttsDao {

    private static final String MSG = "SomeMessage";

    private RecordsService recordsService;

    @BeforeAll
    void init() {
        RecordsServiceFactory factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();

        recordsService.register(this);
    }

    @Test
    void test() {

        List<EntityRef> refs = Collections.singletonList(EntityRef.create("test", ""));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            recordsService.getAtts(refs, Collections.singleton("str"));
        });

        Throwable rootCause = exception.getCause();
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        assertEquals("state", exception.getMessage());
        assertEquals(MSG, rootCause.getMessage());
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
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

    @NotNull
    @Override
    public String getId() {
        return "test";
    }
}
