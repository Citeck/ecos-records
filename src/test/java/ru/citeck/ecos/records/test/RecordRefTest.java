package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.citeck.ecos.records2.RecordRef;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RecordRefTest {

    @ParameterizedTest
    @CsvSource({"1,2,3",
                "alfresco,,workspace://SpacesStore/123213-123123-123123-123",
                ",tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                "alfresco,tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                ",,workspace://SpacesStore/..."})
    void test(String appName, String sourceId, String id) throws IOException {

        RecordRef recordRef = new RecordRef(appName, sourceId, id);

        if (appName == null) {
            appName = "";
        }
        if (sourceId == null) {
            sourceId = "";
        }
        if (id == null) {
            id = "";
        }

        assertEquals(recordRef.getAppName(), appName);
        assertEquals(recordRef.getSourceId(), sourceId);
        assertEquals(recordRef.getId(), id);
        if (!appName.isEmpty()) {
            assertEquals(appName + "/" + sourceId + "@" + id, recordRef.toString());
        } else if (!sourceId.isEmpty()) {
            assertEquals(sourceId + "@" + id, recordRef.toString());
        } else {
            assertEquals(id, recordRef.toString());
        }

        assertEquals(recordRef, new RecordRef(recordRef.toString()));

        RecordRef otherRef = new RecordRef(appName + "0", sourceId, id);
        assertNotEquals(otherRef, recordRef);

        ObjectMapper mapper = new ObjectMapper();
        String otherRefStr = mapper.writeValueAsString(otherRef);
        assertEquals("\"" + otherRef.toString() + "\"", otherRefStr);

        RecordRef remappedOtherRef = mapper.readValue(otherRefStr, RecordRef.class);
        assertEquals(otherRef, remappedOtherRef);
    }
}
