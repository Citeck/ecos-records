package ru.citeck.ecos.records.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordRef;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordRefTest {

    @ParameterizedTest
    @CsvSource({"1,2,3",
                "alfresco,,workspace://SpacesStore/123213-123123-123123-123",
                ",tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                "alfresco,tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                ",,workspace://SpacesStore/...",
                ",123,"})
    void test(String appName, String sourceId, String id) {

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

        RecordsObj recordsObj = Json.getMapper().read("{\"record\": \"" + refFromStr + "\"}", RecordsObj.class);
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

        String otherRefStr = Json.getMapper().toString(otherRef);
        assertEquals("\"" + otherRef.toString() + "\"", otherRefStr);

        RecordRef remappedOtherRef = Json.getMapper().convert(otherRefStr, RecordRef.class);
        assertEquals(otherRef, remappedOtherRef);

        RecordsList list = Json.getMapper().read("[\"\", \"\"]", RecordsList.class);
        assertEquals(2, list.size());
        assertSame(list.get(0), RecordRef.EMPTY);
        assertSame(list.get(1), RecordRef.EMPTY);
    }

    @Test
    void defaultSourceTest() {

        String sourceId = "source-id";
        RecordRef ref0 = RecordRef.valueOf(sourceId + "@@");
        RecordRef ref1 = RecordRef.valueOf(sourceId + "@@@@@");

        assertEquals(ref0, ref1);
        assertEquals(sourceId, ref0.getSourceId());
        assertEquals(sourceId, ref1.getSourceId());

        assertEquals(ref0, RecordRef.create(null, sourceId, "@@@@"));
        assertEquals(ref0, RecordRef.create(null, sourceId, "@"));
        assertEquals(ref0, RecordRef.create(null, sourceId, null));
        assertEquals(RecordRef.valueOf("some-app/" + sourceId + "@"),
                     RecordRef.create("some-app", sourceId, null));
    }

    private static class RecordsList extends ArrayList<RecordRef> {
    }

    static class RecordsObj {

        List<RecordRef> list;

        public void setRecord(RecordRef record) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(record);
        }
    }
}
