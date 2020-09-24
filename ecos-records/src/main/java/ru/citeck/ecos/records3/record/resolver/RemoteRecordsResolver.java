package ru.citeck.ecos.records3.record.resolver;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.delete.request.RecordsDelResult;
import ru.citeck.ecos.records3.record.operation.delete.request.RecordsDeletion;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutation;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.record.operation.query.typed.RecsMetaQueryRes;
import ru.citeck.ecos.records3.record.operation.delete.request.DeletionBody;
import ru.citeck.ecos.records3.record.operation.mutate.MutateBody;
import ru.citeck.ecos.records3.rest.QueryBody;
import ru.citeck.ecos.records3.request.result.RecordsResult;
import ru.citeck.ecos.records3.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;
import ru.citeck.ecos.records3.utils.RecordsUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class RemoteRecordsResolver implements RecordsResolver {

    public static final String BASE_URL = "/api/records/";
    public static final String QUERY_URL = BASE_URL + "query";
    public static final String MUTATE_URL = BASE_URL + "mutate";
    public static final String DELETE_URL = BASE_URL + "delete";

    private final RemoteRecordsRestApi restApi;
    private String defaultAppName = "";

    private final Map<String, String> sourceIdMapping = new HashMap<>();

    public RemoteRecordsResolver(RecordsServiceFactory factory, RemoteRecordsRestApi restApi) {
        this.restApi = restApi;

        Map<String, String> sourceIdMapping = factory.getProperties().getSourceIdMapping();
        if (sourceIdMapping != null) {
            this.sourceIdMapping.putAll(sourceIdMapping);
        }
    }

    @Override
    public RecsQueryRes<RecordMeta> queryRecords(@NotNull RecordsQuery query,
                                                 @NotNull Map<String, String> attributes,
                                                 boolean rawAtts) {

        String sourceId = query.getSourceId();
        if (sourceId.indexOf('/') == -1) {
            sourceId = defaultAppName + "/" + sourceId;
        }

        sourceId = sourceIdMapping.getOrDefault(sourceId, sourceId);

        String appName;

        RecordsQuery appQuery = new RecordsQuery(query);

        int appDelimIdx = sourceId.indexOf("/");
        appName = sourceId.substring(0, appDelimIdx);
        appQuery.setSourceId(sourceId.substring(appDelimIdx + 1));

        String url = "/" + appName + QUERY_URL;

        QueryBody queryBody = new QueryBody();

        queryBody.setQuery(appQuery);
        queryBody.setAttributes(attributes);
        queryBody.setRawAtts(rawAtts);

        RecsMetaQueryRes appResult = restApi.jsonPost(url, queryBody, RecsMetaQueryRes.class);

        return RecordsUtils.metaWithDefaultApp(appResult, appName);
    }

    @Override
    public List<RecordMeta> getMeta(@NotNull List<RecordRef> records,
                                    @NotNull Map<String, String> attributes,
                                    boolean rawAtts) {

        return execRecordsAppRequest(
            records,
            QUERY_URL,
            RecordRef::getAppName,
            RecordRef::removeAppName,
            this::addAppName,
            appRecords -> {
                QueryBody queryBody = new QueryBody();
                queryBody.setRecords(appRecords);
                queryBody.setAttributes(attributes);
                queryBody.setRawAtts(rawAtts);
                return queryBody;
            },
            RecsMetaQueryRes.class
        ).getRecords();
    }

    @Override
    public RecordsMutResult mutate(@NotNull RecordsMutation mutation) {
        return this.execRecordsAppRequest(
            mutation.getRecords(),
            MUTATE_URL,
            m -> m.getId().getAppName(),
            this::removeAppName,
            this::addAppName,
            appRecords -> {
                MutateBody body = new MutateBody();
                body.setRecords(appRecords);
                if (mutation.isDebug()) {
                    body.setDebug(true);
                }
                return body;
            },
            RecordsMutResult.class
        );
    }

    @Nullable
    @Override
    public RecsSourceInfo getSourceInfo(@NotNull String sourceId) {
        //todo
        return null;
    }

    @NotNull
    @Override
    public List<RecsSourceInfo> getSourceInfo() {
        //todo
        return Collections.emptyList();
    }

    private RecordMeta addAppName(RecordMeta meta, String app) {
        return new RecordMeta(meta, r -> r.addAppName(app));
    }

    private RecordMeta removeAppName(RecordMeta meta) {
        return new RecordMeta(meta, RecordRef::removeAppName);
    }

    @NotNull
    @Override
    public RecordsDelResult delete(@NotNull RecordsDeletion deletion) {
        return execRecordsAppRequest(
            deletion.getRecords(),
            DELETE_URL,
            RecordRef::getAppName,
            RecordRef::removeAppName,
            this::addAppName,
            appRecords -> {
                DeletionBody body = new DeletionBody();
                body.setRecords(appRecords);
                return body;
            },
            RecordsDelResult.class
        );
    }

    private <I, O, R extends RecordsResult<I>> R execRecordsAppRequest(
                                                              List<O> records,
                                                              String url,
                                                              Function<O, String> getApp,
                                                              Function<O, O> removeApp,
                                                              BiFunction<I, String, I> setApp,
                                                              Function<List<O>, Object> bodySupplier,
                                                              Class<R> respType) {

        String appName = null;
        List<O> requestRecords = new ArrayList<>();

        R result;
        try {
            result = respType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Error!", e);
        }

        for (O ref : records) {

            String refAppName = getApp.apply(ref);
            if (refAppName.isEmpty()) {
                refAppName = defaultAppName;
            }

            if (appName == null) {

                appName = refAppName;

            } else if (!refAppName.equals(appName)) {

                Object body = bodySupplier.apply(requestRecords);
                result.merge(postRecords(appName, url, body, setApp, respType));

                appName = null;
                requestRecords.clear();
            }

            requestRecords.add(removeApp.apply(ref));
        }

        if (!requestRecords.isEmpty()) {
            Object body = bodySupplier.apply(requestRecords);
            result.merge(postRecords(appName, url, body, setApp, respType));
        }

        return result;
    }

    private <T> RecordsResult<T> postRecords(String appName,
                                             String url,
                                             Object body,
                                             BiFunction<T, String, T> setApp,
                                             Class<? extends RecordsResult<T>> respType) {

        String appUrl = "/" + appName + url;

        RecordsResult<T> appResult = restApi.jsonPost(appUrl, body, respType);

        appResult.setRecords(appResult.getRecords()
                                      .stream()
                                      .map(r -> setApp.apply(r, appName))
                                      .collect(Collectors.toList()));

        return appResult;
    }

    public void setDefaultAppName(String defaultAppName) {
        this.defaultAppName = defaultAppName;
    }
}
