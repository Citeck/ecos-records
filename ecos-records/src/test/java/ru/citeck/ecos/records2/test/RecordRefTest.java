package ru.citeck.ecos.records2.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

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

        EntityRef recordRef = EntityRef.create(appName, sourceId, id);

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

        EntityRef refFromStr = EntityRef.valueOf(recordStr);
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
        assertEquals(recordRef.getLocalId(), id);
        if (!appName.isEmpty()) {
            assertEquals(appName + "/" + sourceId + "@" + id, recordRef.toString());
        } else if (!sourceId.isEmpty()) {
            assertEquals(sourceId + "@" + id, recordRef.toString());
        } else {
            assertEquals(id, recordRef.toString());
        }

        assertEquals(recordRef, EntityRef.valueOf(recordRef.toString()));

        EntityRef otherRef = EntityRef.create(appName + "0", sourceId, id);
        assertNotEquals(otherRef, recordRef);

        String otherRefStr = Json.getMapper().toString(otherRef);
        assertEquals("\"" + otherRef + "\"", otherRefStr);

        EntityRef remappedOtherRef = Json.getMapper().convert(otherRefStr, EntityRef.class);
        assertEquals(otherRef, remappedOtherRef);

        RecordsList list = Json.getMapper().read("[\"\", \"\"]", RecordsList.class);
        assertEquals(2, list.size());
        assertSame(list.get(0), EntityRef.EMPTY);
        assertSame(list.get(1), EntityRef.EMPTY);
    }

    @Test
    void defaultSourceTest() {

        String sourceId = "source-id";
        EntityRef ref0 = EntityRef.valueOf(sourceId + "@@");
        EntityRef ref1 = EntityRef.valueOf(sourceId + "@@@@@");

        assertEquals(sourceId, ref0.getSourceId());
        assertEquals(sourceId, ref1.getSourceId());

        assertEquals("source-id@@@@@", EntityRef.create(null, sourceId, "@@@@").toString());
        assertEquals("source-id@@", EntityRef.create(null, sourceId, "@").toString());
        assertEquals("source-id@", EntityRef.create(null, sourceId, null).toString());
        assertEquals(EntityRef.valueOf("some-app/" + sourceId + "@"),
                     EntityRef.create("some-app", sourceId, null));
    }

    private static class RecordsList extends ArrayList<EntityRef> {
    }

    static class RecordsObj {

        List<EntityRef> list;

        public void setRecord(EntityRef record) {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(record);
        }
    }
}
