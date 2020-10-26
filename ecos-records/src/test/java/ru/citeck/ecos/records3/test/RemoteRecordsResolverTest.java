package ru.citeck.ecos.records3.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteRecordsResolverTest {

    private static final String DEFAULT_APP = "alf";

    static class Factory extends RecordsServiceFactory {

        RemoteRecordsRestApi restApi;

        Factory(RemoteRecordsRestApi connection) {
            this.restApi = connection;
        }

        @Override
        protected RemoteRecordsResolver createRemoteRecordsResolver() {
            RemoteRecordsResolver resolver = new RemoteRecordsResolver(this, restApi);
            resolver.setDefaultAppName(DEFAULT_APP);
            return resolver;
        }
    }

    private RecordsService recordsService;

    private final List<RecordRef> refs = new ArrayList<>();
    private final Map<RecordRef, RecordAtts> metaByRef = new HashMap<>();

    private final List<String> urls = new ArrayList<>();

    @BeforeAll
    void init() {

        Factory factory = new Factory(RemoteRecordsResolverTest.this::jsonPost);

        recordsService = factory.getRecordsServiceV1();

        refs.add(RecordRef.valueOf("src1@loc1"));
        refs.add(RecordRef.valueOf("src1@loc2"));
        refs.add(RecordRef.valueOf("uiserv/src2@loc3"));
        refs.add(RecordRef.valueOf("uiserv/src2@loc4"));
        refs.add(RecordRef.valueOf("alf/src2@loc5"));
        refs.add(RecordRef.valueOf("alf/src3@loc6"));

        refs.forEach(r -> {
            RecordRef localRef = r.removeAppName();
            metaByRef.put(localRef, new RecordMeta(localRef));
        });
    }

    private <T> T jsonPost(String url, Object request, Class<T> resultType) {

        urls.add(url);

        if (request instanceof QueryBody) {

            QueryBody body = (QueryBody) request;

            if (body.getRecords() != null) {

                for (RecordRef ref : body.getRecords()) {
                    assertTrue(refs.stream().map(RecordRef::removeAppName).anyMatch(ref::equals));
                }

                RecsQueryRes<RecordAtts> result = new RecsQueryRes<>();
                result.setRecords(body.getRecords().stream().map(metaByRef::get).collect(Collectors.toList()));
                return Json.getMapper().convert(result, resultType);

            } else if (body.getQuery() != null) {

                assertFalse(body.getQuery().getSourceId().contains("/"));
                List<RecordAtts> result = new ArrayList<>(metaByRef.values());
                RecordsQueryResult<RecordAtts> queryResult = new RecordsQueryResult<>();
                queryResult.setRecords(result);
                return Json.getMapper().convert(queryResult, resultType);

            } else {
                throw new IllegalStateException("Incorrect query: " + request);
            }
        } else if (request instanceof MutationBody) {

            MutationBody body = (MutationBody) request;

            RecordsMutResult result = new RecordsMutResult();
            result.setRecords(body.getRecords()
                .stream()
                .map(r -> new RecordMeta(metaByRef.get(r.getId())))
                .collect(Collectors.toList()));
            return Json.getMapper().convert(result, resultType);

        } else if (request instanceof DeletionBody) {

            DeletionBody body = (DeletionBody) request;

            RecordsMutResult result = new RecordsMutResult();
            result.setRecords(body.getRecords()
                .stream()
                .map(r -> new RecordMeta(metaByRef.get(r)))
                .collect(Collectors.toList()));
            return Json.getMapper().convert(result, resultType);

        } else {
            throw new IllegalArgumentException("Body type is unknown: " + request + " "
                                               + (request != null ? request.getClass() : null));
        }
    }

    @Test
    void test() {

        urls.clear();

        String appId = "some-app";

        RecordsQuery query = new RecordsQuery();
        query.setSourceId(appId + "/localSource");

        RecsQueryRes<RecordRef> result = recordsService.query(query);
        assertEquals(refs.stream()
                        .map(r -> RecordRef.valueOf(appId + "/" + r.removeAppName().toString()))
                        .collect(Collectors.toList()),
                     result.getRecords());

        assertEquals(1, urls.size());
        assertEquals("/" + appId + RemoteRecordsResolver.QUERY_URL, urls.get(0));

        urls.clear();

        List<RecordRef> qrefs = new ArrayList<>(refs);
        List<RecordAtts> metaResult = recordsService.getAtts(qrefs, Collections.singletonMap("aa", "bb"));

        assertEquals(3, urls.size());
        assertEquals(qrefs, metaResult.stream().map(RecordAtts::getId).collect(Collectors.toList()));

        //todo
       /* List<RecordRef> mutResult = recordsService.mutate(refs.stream().map(RecordAtts::new).collect(Collectors.toList()));
        checkRecordsMeta(Collections.singletonList(refs.get(0)), mutResult.stream().map(RecordAtts::new).collect(Collectors.toList()));

        List<DelStatus> delResult = recordsService.delete(refs);
        assertEquals(refs.size(), delResult.size());*/
    }

    private void checkRecordsMeta(List<RecordRef> expected, List<RecordAtts> records) {
        assertEquals(expected.size(), records.size());
        assertEquals(expected.stream().map(r -> {
                if (r.getAppName().isEmpty()) {
                    return r.addAppName(DEFAULT_APP);
                }
                return r;
            }).collect(Collectors.toList()),
            records.stream().map(RecordAtts::getId).collect(Collectors.toList()));
    }
}
