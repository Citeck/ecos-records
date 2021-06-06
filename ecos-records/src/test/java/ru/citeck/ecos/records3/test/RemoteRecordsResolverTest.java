package ru.citeck.ecos.records3.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.RecordsProperties;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteRecordsResolverTest {

    private static final String DEFAULT_APP = "alf";

    class Factory extends RecordsServiceFactory {

        Factory() {
        }

        @Override
        public RemoteRecordsResolver createRemoteRecordsResolver() {
            RemoteRecordsResolver resolver = new RemoteRecordsResolver(this,
                RemoteRecordsResolverTest.this::jsonPost);
            resolver.setDefaultAppName(DEFAULT_APP);
            return resolver;
        }

        @Override
        protected RecordsProperties createProperties() {
            RecordsProperties props = super.createProperties();
            props.setGatewayMode(true);
            return props;
        }
    }

    private static final String TEST_ATT = ".att(n:\"test\"){str}";

    private RecordsService recordsService;

    private final List<RecordRef> refs = new ArrayList<>();
    private final Map<RecordRef, RecordMeta> metaByRef = new HashMap<>();

    private final List<String> urls = new ArrayList<>();

    @BeforeAll
    void init() {

        Factory factory = new Factory();

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

                for (RecordRef ref : body.getRecords()) {
                    assertTrue(refs.stream().map(RecordRef::removeAppName).anyMatch(ref::equals));
                }

                RecordsQueryResult<RecordMeta> result = new RecordsQueryResult<>();
                result.setRecords(body.getRecords().stream().map(metaByRef::get).collect(Collectors.toList()));
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
            result.setRecords(body.getRecords().stream().map(metaByRef::get).collect(Collectors.toList()));
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

        ru.citeck.ecos.records2.request.query.RecordsQuery query = new RecordsQuery();
        query.setSourceId(appId + "/localSource");

        RecordsQueryResult<RecordRef> result = recordsService.queryRecords(query);
        assertEquals(refs.stream()
                .map(r -> RecordRef.valueOf(appId + "/" + r.removeAppName().toString()))
                .collect(Collectors.toList()),
            result.getRecords());

        assertEquals(1, urls.size());
        assertEquals("/" + appId + RemoteRecordsResolver.QUERY_URL, urls.get(0));

        urls.clear();

        List<RecordRef> qrefs = new ArrayList<>(refs);
        RecordsResult<RecordMeta> metaResult = recordsService.getAttributes(qrefs, Collections.singleton(TEST_ATT));

        assertEquals(4, urls.size());
        checkRecordsMeta(refs, metaResult.getRecords(), false);

        RecordsMutation mutation = new RecordsMutation();
        mutation.setRecords(refs.stream().map(RecordMeta::new).collect(Collectors.toList()));

        RecordsMutResult mutResult = recordsService.mutate(mutation);
        checkRecordsMeta(Collections.singletonList(refs.get(0)), mutResult.getRecords(), true);

        RecordsDeletion deletion = new RecordsDeletion();
        deletion.setRecords(refs);

        RecordsDelResult delResult = recordsService.delete(deletion);
        checkRecordsMeta(refs, delResult.getRecords(), false);
    }

    private void checkRecordsMeta(List<RecordRef> expected, List<RecordMeta> records, boolean addDefaultAppName) {
        assertEquals(expected.size(), records.size());
        assertEquals(expected.stream().map(r -> {
                if (r.getAppName().isEmpty() && addDefaultAppName) {
                    return r.addAppName(DEFAULT_APP);
                }
                return r;
            }).collect(Collectors.toList()),
            records.stream().map(RecordMeta::getId).collect(Collectors.toList()));
    }
}
