package ru.citeck.ecos.records3.test;

import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//todo
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    private static final String TEST_SCHEMA = "!schema!";

    private RecordsService recordsService;

    private List<RecordRef> refs = new ArrayList<>();
    private Map<RecordRef, RecordAtts> metaByRef = new HashMap<>();

    private List<String> urls = new ArrayList<>();

/*    @BeforeAll
    void init() {

        Factory factory = new Factory(new RestApi(this::jsonPost, factory.getProperties()));

        recordsService = factory.getRecordsService();

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

                assertEquals(TEST_SCHEMA, body.getSchema());

                for (RecordRef ref : body.getRecords()) {
                    assertTrue(refs.stream().map(RecordRef::removeAppName).anyMatch(ref::equals));
                }

                RecordsQueryResult<RecordMeta> result = new RecordsQueryResult<>();
                result.setRecords(body.getRecords().stream().map(r -> metaByRef.get(r)).collect(Collectors.toList()));
                return Json.getMapper().convert(result, resultType);

            } else if (body.getQuery() != null) {

                assertFalse(body.getQuery().getSourceId().contains("/"));
                RecordsResult<RecordMeta> result = new RecordsResult<>();
                result.setRecords(new ArrayList<>(metaByRef.values()));
                return Json.getMapper().convert(result, resultType);

            } else {
                throw new IllegalStateException("Incorrect query: " + request);
            }
        } else if (request instanceof MutationBody) {

            MutationBody body = (MutationBody) request;

            RecordsMutResult result = new RecordsMutResult();
            result.setRecords(body.getRecords().stream().map(r -> metaByRef.get(r.getId())).collect(Collectors.toList()));
            return Json.getMapper().convert(result, resultType);

        } else if (request instanceof DeletionBody) {

            DeletionBody body = (DeletionBody) request;

            RecordsMutResult result = new RecordsMutResult();
            result.setRecords(body.getRecords().stream().map(r -> metaByRef.get(r)).collect(Collectors.toList()));
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

        RecordsQueryResult<RecordRef> result = recordsService.query(query);
        assertEquals(refs.stream()
                        .map(r -> RecordRef.valueOf(appId + "/" + r.removeAppName().toString()))
                        .collect(Collectors.toList()),
                     result.getRecords());

        assertEquals(1, urls.size());
        assertEquals("/" + appId + RemoteRecordsResolver.QUERY_URL, urls.get(0));

        urls.clear();

        List<RecordRef> qrefs = new ArrayList<>(refs);
        RecordsResult<RecordMeta> metaResult = recordsService.getAtts(qrefs, TEST_SCHEMA);

        assertEquals(3, urls.size());
        checkRecordsMeta(refs, metaResult.getRecords());

        RecordsMutation mutation = new RecordsMutation();
        mutation.setRecords(refs.stream().map(RecordMeta::new).collect(Collectors.toList()));

        RecordsMutResult mutResult = recordsService.mutate(mutation);
        checkRecordsMeta(Collections.singletonList(refs.get(0)), mutResult.getRecords());

        RecordsDeletion deletion = new RecordsDeletion();
        deletion.setRecords(refs);

        RecordsDelResult delResult = recordsService.delete(deletion);
        checkRecordsMeta(refs, delResult.getRecords());
    }

    private void checkRecordsMeta(List<RecordRef> expected, List<RecordMeta> records) {
        assertEquals(expected.size(), records.size());
        assertEquals(expected.stream().map(r -> {
                if (r.getAppName().isEmpty()) {
                    return r.addAppName(DEFAULT_APP);
                }
                return r;
            }).collect(Collectors.toList()),
            records.stream().map(RecordMeta::getId).collect(Collectors.toList()));
    }*/
}
