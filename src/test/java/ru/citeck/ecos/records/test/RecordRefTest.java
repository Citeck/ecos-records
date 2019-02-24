package ru.citeck.ecos.records.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RecordRefTest {

    @ParameterizedTest
    @CsvSource({"1,2,3",
                "alfresco,,workspace://SpacesStore/123213-123123-123123-123",
                ",tax-rep,workspace://SpacesStore/123213-21321-312321-213",
                "alfresco,tax-rep,workspace://SpacesStore/123213-21321-312321-213"})
    void test(String appName, String sourceId, String id) {

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
    }
}
