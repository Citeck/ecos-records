package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.citeck.ecos.records2.RecordRef;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RecordRefTest {

    @ParameterizedTest
    @CsvSource({"1,2,3",
                "alfresco,,workspace://SpacesStore/123213-123123-123123-123",
                ",tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                "alfresco,tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                ",,workspace://SpacesStore/...",
                ",123,"})
    void test(String appName, String sourceId, String id) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        RecordRef recordRef = RecordRef.create(appName, sourceId, id);

        String recordStr = "";
        if (appName != null) {
            recordStr = appName + "/";
        }
        if (sourceId != null) {
            recordStr += sourceId;
        }
        if (!recordStr.isEmpty()) {
            recordStr += "@";
        }
        recordStr += id != null ? id : "";

        RecordRef refFromStr = RecordRef.valueOf(recordStr);
        assertEquals(recordRef, refFromStr);

        RecordsObj recordsObj = mapper.readValue("{\"record\": \"" + refFromStr + "\"}", RecordsObj.class);
        assertEquals(recordRef, recordsObj.list.get(0));

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

        assertEquals(recordRef, RecordRef.valueOf(recordRef.toString()));

        RecordRef otherRef = RecordRef.create(appName + "0", sourceId, id);
        assertNotEquals(otherRef, recordRef);

        String otherRefStr = mapper.writeValueAsString(otherRef);
        assertEquals("\"" + otherRef.toString() + "\"", otherRefStr);

        RecordRef remappedOtherRef = mapper.readValue(otherRefStr, RecordRef.class);
        assertEquals(otherRef, remappedOtherRef);

        RecordsList list = mapper.readValue("[\"\", \"\"]", RecordsList.class);
        assertEquals(2, list.size());
        assertSame(list.get(0), RecordRef.EMPTY);
        assertSame(list.get(1), RecordRef.EMPTY);
    }

    static class RecordsList extends ArrayList<RecordRef> {
    }

    static class RecordsObj {

        public List<RecordRef> list;

        public void setRecord(RecordRef record) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(record);
        }
    }
}
