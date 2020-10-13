package ru.citeck.ecos.records3.record.resolver;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.delete.DeleteBody;
import ru.citeck.ecos.records3.record.operation.delete.DeleteResp;
import ru.citeck.ecos.records3.record.operation.mutate.MutateBody;
import ru.citeck.ecos.records3.record.operation.mutate.MutateResp;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.operation.query.dto.typed.RecordsMetaQueryRes;
import ru.citeck.ecos.records3.rest.v1.QueryBody;
import ru.citeck.ecos.records3.rest.QueryResp;
import ru.citeck.ecos.records3.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;
import ru.citeck.ecos.records3.utils.RecordsUtils;
import ru.citeck.ecos.records3.utils.ValueWithIdx;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RemoteRecordsResolver {

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

    public RecordsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
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

        RecordsMetaQueryRes appResult = restApi.jsonPost(url, queryBody, RecordsMetaQueryRes.class);

        return RecordsUtils.metaWithDefaultApp(appResult, appName);
    }

    public List<RecordAtts> getAtts(@NotNull List<RecordRef> records,
                                    @NotNull Map<String, String> attributes,
                                    boolean rawAtts) {

        List<ValueWithIdx<RecordAtts>> result = new ArrayList<>();

        Map<String, List<ValueWithIdx<RecordRef>>> refsByApp = RecordsUtils.groupByApp(records);
        refsByApp.forEach((app, refs) -> {

            if (StringUtils.isBlank(app)) {
                app = defaultAppName;
            }
            QueryBody queryBody = new QueryBody();
            queryBody.setRecords(refs.stream().map(ValueWithIdx::getValue).collect(Collectors.toList()));
            queryBody.setAttributes(attributes);
            queryBody.setRawAtts(rawAtts);

            QueryResp queryResp = postRecords(app, QUERY_URL, queryBody, QueryResp.class);

            if (queryResp == null || queryResp.getRecords() == null || queryResp.getRecords().size() != refs.size()) {
                log.error("Incorrect response: " + queryBody + "\n query: " + queryBody);
                for (ValueWithIdx<RecordRef> ref : refs) {
                    result.add(new ValueWithIdx<>(new RecordAtts(ref.getValue()), ref.getIdx()));
                }
            } else {
                List<RecordAtts> recsAtts = queryResp.getRecords();
                for (int i = 0; i < refs.size(); i++) {
                    ValueWithIdx<RecordRef> ref = refs.get(i);
                    RecordAtts atts = recsAtts.get(i);
                    result.add(new ValueWithIdx<>(new RecordAtts(atts, ref.getValue()), ref.getIdx()));
                }
            }
        });

        result.sort(Comparator.comparingInt(ValueWithIdx::getIdx));
        return result.stream().map(ValueWithIdx::getValue).collect(Collectors.toList());
    }

    public List<RecordRef> mutate(@NotNull List<RecordAtts> records) {

        List<ValueWithIdx<RecordRef>> result = new ArrayList<>();

        Map<String, List<ValueWithIdx<RecordAtts>>> attsByApp = RecordsUtils.groupAttsByApp(records);
        attsByApp.forEach((app, atts) -> {

            if (StringUtils.isBlank(app)) {
                app = defaultAppName;
            }
            MutateBody mutateBody = new MutateBody();
            mutateBody.setRecords(atts.stream().map(ValueWithIdx::getValue).collect(Collectors.toList()));

            MutateResp mutResp = postRecords(app, MUTATE_URL, mutateBody, MutateResp.class);

            if (mutResp == null || mutResp.getRecords() == null || mutResp.getRecords().size() != atts.size()) {
                log.error("Incorrect response: " + mutResp + "\n query: " + mutateBody);
                for (ValueWithIdx<RecordAtts> att : atts) {
                    result.add(new ValueWithIdx<>(att.getValue().getId(), att.getIdx()));
                }
            } else {
                List<RecordAtts> recsAtts = mutResp.getRecords();
                for (int i = 0; i < atts.size(); i++) {
                    ValueWithIdx<RecordAtts> refAtts = atts.get(i);
                    RecordAtts respAtts = recsAtts.get(i);
                    result.add(new ValueWithIdx<>(respAtts.getId(), refAtts.getIdx()));
                }
            }
        });

        result.sort(Comparator.comparingInt(ValueWithIdx::getIdx));
        return result.stream().map(ValueWithIdx::getValue).collect(Collectors.toList());
    }

    @Nullable
    public RecsSourceInfo getSourceInfo(@NotNull String sourceId) {
        //todo
        return null;
    }

    @NotNull
    public List<RecsSourceInfo> getSourceInfo() {
        //todo
        return Collections.emptyList();
    }

    private RecordAtts addAppName(RecordAtts meta, String app) {
        return new RecordAtts(meta, r -> r.addAppName(app));
    }

    private RecordAtts removeAppName(RecordAtts meta) {
        return new RecordAtts(meta, RecordRef::removeAppName);
    }

    @NotNull
    public List<DelStatus> delete(@NotNull List<RecordRef> records) {

        List<ValueWithIdx<DelStatus>> result = new ArrayList<>();

        Map<String, List<ValueWithIdx<RecordRef>>> attsByApp = RecordsUtils.groupByApp(records);
        attsByApp.forEach((app, refs) -> {

            if (StringUtils.isBlank(app)) {
                app = defaultAppName;
            }
            DeleteBody deleteBody = new DeleteBody();
            deleteBody.setRecords(refs.stream().map(ValueWithIdx::getValue).collect(Collectors.toList()));

            DeleteResp delResp = postRecords(app, DELETE_URL, deleteBody, DeleteResp.class);

            List<DelStatus> statues = toDelStatuses(refs.size(), delResp);

            for (int i = 0; i < refs.size(); i++) {
                ValueWithIdx<RecordRef> refAtts = refs.get(i);
                DelStatus status = statues.get(i);
                result.add(new ValueWithIdx<>(status, refAtts.getIdx()));
            }
        });

        result.sort(Comparator.comparingInt(ValueWithIdx::getIdx));
        return result.stream().map(ValueWithIdx::getValue).collect(Collectors.toList());
    }

    private List<DelStatus> toDelStatuses(int expectedSize, DeleteResp resp) {

        if (resp == null) {
            return getStatuses(expectedSize, DelStatus.ERROR);
        }

        if (resp.getStatuses() != null && resp.getStatuses().size() == expectedSize) {
            return resp.getStatuses();
        }

        if (resp.getRecords() != null && resp.getRecords().size() == expectedSize) {
            return getStatuses(expectedSize, DelStatus.OK);
        }

        return getStatuses(expectedSize, DelStatus.ERROR);
    }

    private List<DelStatus> getStatuses(int size, DelStatus status) {
        return Stream.generate(() -> status)
            .limit(size)
            .collect(Collectors.toList());
    }

    @Nullable
    private <T> T postRecords(String appName, String url, Object body, Class<T> respType) {
        String appUrl = "/" + appName + url;
        return restApi.jsonPost(appUrl, body, respType);
    }

    public void setDefaultAppName(String defaultAppName) {
        this.defaultAppName = defaultAppName;
    }
}
