package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaQueryResult;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.remote.RecordsRestConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RemoteRecordsResolver implements RecordsResolver {

    public static final String BASE_URL = "/api/records/";
    public static final String QUERY_URL = BASE_URL + "query";
    public static final String MUTATE_URL = BASE_URL + "mutate";
    public static final String DELETE_URL = BASE_URL + "delete";

    private RecordsRestConnection restConnection;
    private String defaultAppName = "";

    public RemoteRecordsResolver(RecordsRestConnection restConnection) {
        this.restConnection = restConnection;
    }

    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

        String sourceId = query.getSourceId();
        String appName;

        RecordsQuery appQuery = new RecordsQuery(query);

        int appDelimIdx = sourceId.indexOf("/");
        if (appDelimIdx >= 0) {
            appName = sourceId.substring(0, appDelimIdx);
            appQuery.setSourceId(sourceId.substring(appDelimIdx + 1));
        } else {
            appName = defaultAppName;
        }

        String url = "/" + appName + QUERY_URL;

        QueryBody queryBody = new QueryBody();

        queryBody.setQuery(appQuery);
        queryBody.setSchema(schema == null ? "" : schema);

        RecordsMetaQueryResult appResult = restConnection.jsonPost(url, queryBody, RecordsMetaQueryResult.class);

        return appResult.addAppName(appName);
    }

    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        return execRecordsAppRequest(
            records,
            QUERY_URL,
            RecordRef::getAppName,
            RecordRef::removeAppName,
            this::addAppName,
            appRecords -> {
                QueryBody queryBody = new QueryBody();
                queryBody.setRecords(appRecords);
                queryBody.setSchema(schema);
                return queryBody;
            },
            RecordsMetaQueryResult.class
        );
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {
        return this.execRecordsAppRequest(
            mutation.getRecords(),
            MUTATE_URL,
            m -> m.getId().getAppName(),
            this::removeAppName,
            this::addAppName,
            appRecords -> {
                MutationBody body = new MutationBody();
                body.setRecords(appRecords);
                if (mutation.isDebug()) {
                    body.setDebug(true);
                }
                return body;
            },
            RecordsMutResult.class
        );
    }

    private RecordMeta addAppName(RecordMeta meta, String app) {
        return new RecordMeta(meta, r -> r.addAppName(app));
    }

    private RecordMeta removeAppName(RecordMeta meta) {
        return new RecordMeta(meta, RecordRef::removeAppName);
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
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
                                                              Collection<O> records,
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

        RecordsResult<T> appResult = restConnection.jsonPost(appUrl, body, respType);

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
