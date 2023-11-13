package ru.citeck.ecos.records3.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.test.commons.EcosWebAppApiMock;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.RecordsProperties;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.resolver.RemoteRecordsResolver;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp;
import ru.citeck.ecos.records3.rest.v1.query.QueryBody;
import ru.citeck.ecos.webapp.api.EcosWebAppApi;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteRecordsResolverTest {

    private static final String DEFAULT_APP = "alf";

    class Factory extends RecordsServiceFactory {

        Factory() {
        }

        @Nullable
        @Override
        public EcosWebAppApi getEcosWebAppApi() {
            EcosWebAppApiMock ctx = new EcosWebAppApiMock("test", true);
            ctx.setWebClientExecuteImpl(RemoteRecordsResolverTest.this::jsonPost);
            return ctx;
        }

        @NotNull
        @Override
        protected RecordsProperties createProperties() {
            return super.createProperties().withDefaultApp(DEFAULT_APP);
        }
    }

    private static final String TEST_ATT = ".att(n:\"test\"){str}";

    private RecordsService recordsService;

    private final List<EntityRef> refs = new ArrayList<>();
    private final Map<EntityRef, RecordMeta> metaByRef = new HashMap<>();

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
            EntityRef localRef = r.withoutAppName();
            metaByRef.put(localRef, new RecordMeta(localRef));
        });
    }

    private Object jsonPost(String targetApp, String path, Object request) {

        urls.add("/" + targetApp + path);

        if (path.equals("/records/query")) {

            QueryBody body = Json.getMapper().readNotNull((byte[]) request, QueryBody.class);

            if (body.getRecords() != null) {

                if (body.getRecords().size() == 1 && body.getRecords().get(0).getSourceId().equals("api")) {
                    urls.remove(urls.size() - 1);
                    return null;
                }
                for (EntityRef ref : body.getRecords()) {
                    assertTrue(refs.stream().map(EntityRef::withoutAppName).anyMatch(ref::equals));
                }

                RecordsQueryResult<RecordMeta> result = new RecordsQueryResult<>();
                result.setRecords(body.getRecords().stream().map(metaByRef::get).collect(Collectors.toList()));
                return result;

            } else if (body.getQuery() != null) {

                assertFalse(body.getQuery().getSourceId().contains("/"));
                RecordsResult<RecordMeta> result = new RecordsResult<>();
                result.setRecords(new ArrayList<>(metaByRef.values()));
                return result;

            } else {
                throw new IllegalStateException("Incorrect query: " + request);
            }
        } else if (path.equals("/records/mutate")) {

            MutateBody body = Json.getMapper().readNotNull((byte[]) request, MutateBody.class);

            MutateResp result = new MutateResp();
            result.setRecords(body.getRecords().stream().map(r -> metaByRef.get(r.getId())).collect(Collectors.toList()));
            return result;

        } else if (path.equals("/records/delete")) {

            DeleteBody body = Json.getMapper().readNotNull((byte[]) request, DeleteBody.class);

            DeleteResp result = new DeleteResp();
            result.setStatuses(body.getRecords().stream().map((v) -> DelStatus.OK).collect(Collectors.toList()));
            return result;

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

        RecordsQueryResult<EntityRef> result = recordsService.queryRecords(query);
        assertEquals(refs.stream()
                .map(r -> EntityRef.valueOf(appId + "/" + r.withoutAppName()))
                .collect(Collectors.toList()),
            result.getRecords());

        assertEquals(1, urls.size());
        assertEquals("/" + appId + RemoteRecordsResolver.QUERY_PATH, urls.get(0));

        urls.clear();

        List<EntityRef> qrefs = new ArrayList<>(refs);
        RecordsResult<RecordMeta> metaResult = recordsService.getAttributes(qrefs, Collections.singleton(TEST_ATT));

        assertEquals(4, urls.size());
        checkRecordsMeta(refs, metaResult.getRecords(), false);

        RecordsMutation mutation = new RecordsMutation();
        mutation.setRecords(refs.stream().map(RecordMeta::new).collect(Collectors.toList()));

        RecordsMutResult mutResult = recordsService.mutate(mutation);
        checkRecordsMeta(refs, mutResult.getRecords(), true);

        RecordsDeletion deletion = new RecordsDeletion();
        deletion.setRecords(refs);

        RecordsDelResult delResult = recordsService.delete(deletion);
        checkRecordsMeta(refs, delResult.getRecords(), false);
    }

    private void checkRecordsMeta(List<EntityRef> expected, List<RecordMeta> records, boolean addDefaultAppName) {
        assertEquals(expected.size(), records.size());
        assertEquals(expected.stream().map(r -> {
                if (r.getAppName().isEmpty() && addDefaultAppName) {
                    return r.withAppName(DEFAULT_APP);
                }
                return r;
            }).collect(Collectors.toList()),
            records.stream().map(RecordMeta::getId).collect(Collectors.toList()));
    }
}
