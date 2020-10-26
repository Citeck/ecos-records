package ru.citeck.ecos.records3.record.resolver;

import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.typed.RecordsMetaQueryResult;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.rest.RemoteRecordsRestApi;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.RequestCtxData;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.rest.v1.RequestBody;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp;
import ru.citeck.ecos.records3.rest.v1.query.QueryBody;
import ru.citeck.ecos.records3.rest.v1.query.QueryResp;
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo;
import ru.citeck.ecos.records2.utils.RecordsUtils;
import ru.citeck.ecos.records2.utils.ValWithIdx;
import ru.citeck.ecos.records3.utils.V1ConvUtils;

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

    private final JsonMapper mapper = Json.getMapper();

    public RemoteRecordsResolver(RecordsServiceFactory factory, RemoteRecordsRestApi restApi) {
        this.restApi = restApi;

        Map<String, String> sourceIdMapping = factory.getProperties().getSourceIdMapping();
        if (sourceIdMapping != null) {
            this.sourceIdMapping.putAll(sourceIdMapping);
        }
    }

    public RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                          @NotNull Map<String, ?> attributes,
                                          boolean rawAtts) {

        RequestContext context = RequestContext.getCurrentNotNull();

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

        QueryBody queryBody = new QueryBody();

        queryBody.setQuery(appQuery);
        queryBody.setAttributes(attributes);
        queryBody.setRawAtts(rawAtts);
        setContextProps(queryBody, context);

        QueryResp queryResp = execQuery(appName, queryBody, context);
        RecsQueryRes<RecordAtts> result = new RecsQueryRes<>();
        result.setRecords(queryResp.getRecords());
        result.setTotalCount(queryResp.getTotalCount());
        result.setHasMore(queryResp.isHasMore());

        return RecordsUtils.attsWithDefaultApp(result, appName);
    }

    @NotNull
    private QueryResp execQuery(String appName, QueryBody queryBody, RequestContext context) {

        ru.citeck.ecos.records2.request.rest.QueryBody v0Body = toV0QueryBody(queryBody, context);

        ObjectNode appResultObj = postRecords(appName, QUERY_URL, v0Body);
        QueryResp result = toQueryAttsRes(appResultObj, context);
        if (result == null) {
            result = new QueryResp();
        } else {
            context.addAllMsgs(result.getMessages());
        }

        return result;
    }

    @Nullable
    private QueryResp toQueryAttsRes(ObjectNode body, RequestContext context) {

        if (body == null || body.isEmpty()) {
            return null;
        }

        if (body.path("version").asInt(0) == 1) {
            return mapper.convert(body, QueryResp.class);
        }

        RecordsMetaQueryResult v0Result = mapper.convert(body, RecordsMetaQueryResult.class);
        if (v0Result == null) {
            return null;
        }

        V1ConvUtils.addErrorMessages(v0Result.getErrors(), context);
        V1ConvUtils.addDebugMessage(v0Result, context);

        QueryResp resp = new QueryResp();
        resp.setRecords(v0Result.getRecords()
            .stream()
            .map(RecordAtts::new)
            .collect(Collectors.toList()));
        resp.setHasMore(v0Result.getHasMore());
        resp.setTotalCount(v0Result.getTotalCount());

        return resp;
    }

    private ru.citeck.ecos.records2.request.rest.QueryBody toV0QueryBody(QueryBody body, RequestContext context) {

        ru.citeck.ecos.records2.request.rest.QueryBody v0Body = new ru.citeck.ecos.records2.request.rest.QueryBody();
        v0Body.setAttributes(body.getAttributes());
        v0Body.setRecords(body.getRecords());
        v0Body.setV1Body(body);

        if (body.getQuery() != null) {
            v0Body.setQuery(V1ConvUtils.recsQueryV1ToV0(body.getQuery(), context));
        }

        return v0Body;
    }

    public List<RecordAtts> getAtts(@NotNull List<RecordRef> records,
                                    @NotNull Map<String, ?> attributes,
                                    boolean rawAtts) {

        RequestContext context = RequestContext.getCurrentNotNull();

        List<ValWithIdx<RecordAtts>> result = new ArrayList<>();

        Map<String, List<ValWithIdx<RecordRef>>> refsByApp = RecordsUtils.groupByApp(records);
        refsByApp.forEach((app, refs) -> {

            if (StringUtils.isBlank(app)) {
                app = defaultAppName;
            }
            QueryBody queryBody = new QueryBody();
            queryBody.setRecords(refs.stream().map(ValWithIdx::getValue).collect(Collectors.toList()));
            queryBody.setAttributes(attributes);
            queryBody.setRawAtts(rawAtts);

            QueryResp queryResp = execQuery(app, queryBody, context);

            if (queryResp.getRecords() == null || queryResp.getRecords().size() != refs.size()) {
                log.error("Incorrect response: " + queryBody + "\n query: " + queryBody);
                for (ValWithIdx<RecordRef> ref : refs) {
                    result.add(new ValWithIdx<>(new RecordAtts(ref.getValue()), ref.getIdx()));
                }
            } else {
                List<RecordAtts> recsAtts = queryResp.getRecords();
                for (int i = 0; i < refs.size(); i++) {
                    ValWithIdx<RecordRef> ref = refs.get(i);
                    RecordAtts atts = recsAtts.get(i);
                    result.add(new ValWithIdx<>(new RecordAtts(atts, ref.getValue()), ref.getIdx()));
                }
            }
        });

        result.sort(Comparator.comparingInt(ValWithIdx::getIdx));
        return result.stream().map(ValWithIdx::getValue).collect(Collectors.toList());
    }

    public List<RecordRef> mutate(@NotNull List<RecordAtts> records) {

        RequestContext context = RequestContext.getCurrentNotNull();
        List<ValWithIdx<RecordRef>> result = new ArrayList<>();

        Map<String, List<ValWithIdx<RecordAtts>>> attsByApp = RecordsUtils.groupAttsByApp(records);

        attsByApp.forEach((app, atts) -> {

            if (StringUtils.isBlank(app)) {
                app = defaultAppName;
            }

            MutateBody mutateBody = new MutateBody();
            mutateBody.setRecords(atts.stream()
                .map(ValWithIdx::getValue)
                .collect(Collectors.toList()));

            setContextProps(mutateBody, context);

            MutationBody v0Body = new MutationBody();
            if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                v0Body.setDebug(true);
            }
            v0Body.setRecords(mapper.convert(mutateBody.getRecords(), mapper.getListType(RecordMeta.class)));
            v0Body.setV1Body(mutateBody);

            ObjectNode mutRespObj = postRecords(app, MUTATE_URL, v0Body);
            MutateResp mutateResp = toMutateResp(mutRespObj, context);

            if (mutateResp != null && mutateResp.getMessages() != null) {
                mutateResp.getMessages().forEach(context::addMsg);
            }

            if (mutateResp == null
                    || mutateResp.getRecords() == null
                    || mutateResp.getRecords().size() != atts.size()) {

                context.addMsg(MsgLevel.ERROR, () -> "Incorrect response: " + mutateResp + "\n query: " + mutateBody);
                for (ValWithIdx<RecordAtts> att : atts) {
                    result.add(new ValWithIdx<>(att.getValue().getId(), att.getIdx()));
                }
            } else {
                List<RecordAtts> recsAtts = mutateResp.getRecords();
                for (int i = 0; i < atts.size(); i++) {
                    ValWithIdx<RecordAtts> refAtts = atts.get(i);
                    RecordAtts respAtts = recsAtts.get(i);
                    result.add(new ValWithIdx<>(respAtts.getId(), refAtts.getIdx()));
                }
            }
        });

        result.sort(Comparator.comparingInt(ValWithIdx::getIdx));
        return result.stream().map(ValWithIdx::getValue).collect(Collectors.toList());
    }

    @Nullable
    private MutateResp toMutateResp(ObjectNode body, RequestContext context) {

        if (body == null || body.isEmpty()) {
            return null;
        }

        if (body.path("version").asInt(0) == 1) {
            return mapper.convert(body, MutateResp.class);
        }

        RecordsMutResult v0Result = mapper.convert(body, RecordsMutResult.class);
        if (v0Result == null) {
            return null;
        }

        V1ConvUtils.addErrorMessages(v0Result.getErrors(), context);
        V1ConvUtils.addDebugMessage(v0Result, context);

        MutateResp resp = new MutateResp();
        resp.setRecords(v0Result.getRecords()
            .stream()
            .map(RecordAtts::new)
            .collect(Collectors.toList()));

        return resp;
    }

    private RecordAtts addAppName(RecordAtts meta, String app) {
        return new RecordAtts(meta, r -> r.addAppName(app));
    }

    private RecordAtts removeAppName(RecordAtts meta) {
        return new RecordAtts(meta, RecordRef::removeAppName);
    }

    @NotNull
    public List<DelStatus> delete(@NotNull List<RecordRef> records) {

        RequestContext context = RequestContext.getCurrentNotNull();
        List<ValWithIdx<DelStatus>> result = new ArrayList<>();

        Map<String, List<ValWithIdx<RecordRef>>> attsByApp = RecordsUtils.groupByApp(records);
        attsByApp.forEach((app, refs) -> {

            if (StringUtils.isBlank(app)) {
                app = defaultAppName;
            }

            DeleteBody deleteBody = new DeleteBody();
            deleteBody.setRecords(refs.stream()
                .map(ValWithIdx::getValue)
                .collect(Collectors.toList()));
            setContextProps(deleteBody, context);

            DeletionBody v0Body = new DeletionBody();
            v0Body.setRecords(deleteBody.getRecords());

            if (context.isMsgEnabled(MsgLevel.DEBUG)) {
                v0Body.setDebug(true);
            }
            v0Body.setV1Body(deleteBody);

            ObjectNode delRespObj = postRecords(app, DELETE_URL, v0Body);
            DeleteResp resp = toDeleteResp(delRespObj, context);

            if (resp != null && resp.getMessages() != null) {
                resp.getMessages().forEach(context::addMsg);
            }

            List<DelStatus> statues = toDelStatuses(refs.size(), resp, context);

            for (int i = 0; i < refs.size(); i++) {
                ValWithIdx<RecordRef> refAtts = refs.get(i);
                DelStatus status = statues.get(i);
                result.add(new ValWithIdx<>(status, refAtts.getIdx()));
            }
        });

        result.sort(Comparator.comparingInt(ValWithIdx::getIdx));
        return result.stream().map(ValWithIdx::getValue).collect(Collectors.toList());
    }

    @Nullable
    private DeleteResp toDeleteResp(ObjectNode data, RequestContext context) {

        if (data == null || data.size() == 0) {
            return new DeleteResp();
        }

        if (data.path("version").asInt(0) == 1) {
            return mapper.convert(data, DeleteResp.class);
        }

        RecordsDelResult v0Resp = mapper.convert(data, RecordsDelResult.class);
        if (v0Resp == null) {
            return new DeleteResp();
        }
        List<RecordMeta> records = v0Resp.getRecords();

        DeleteResp resp = new DeleteResp();
        resp.setStatuses(records.stream()
            .map(r -> DelStatus.OK)
            .collect(Collectors.toList()));

        V1ConvUtils.addErrorMessages(v0Resp.getErrors(), context);
        V1ConvUtils.addDebugMessage(v0Resp, context);

        return resp;
    }

    private List<DelStatus> toDelStatuses(int expectedSize, DeleteResp resp, RequestContext context) {

        if (resp == null) {
            return getDelStatuses(expectedSize, DelStatus.ERROR);
        }

        if (resp.getStatuses() != null && resp.getStatuses().size() == expectedSize) {
            return resp.getStatuses();
        }

        context.addMsg(MsgLevel.ERROR, () ->
            "Result statues doesn't match request. "
                + "Expected size: " + expectedSize
                + ". Actual response: " + resp );

        return getDelStatuses(expectedSize, DelStatus.ERROR);
    }

    private List<DelStatus> getDelStatuses(int size, DelStatus status) {
        return Stream.generate(() -> status)
            .limit(size)
            .collect(Collectors.toList());
    }

    private void setContextProps(RequestBody body, RequestContext ctx) {
        RequestCtxData<?> ctxData = ctx.getCtxData();
        body.setMsgLevel(ctxData.getMsgLevel());
        body.setRequestId(ctxData.getRequestId());
        body.setRequestTrace(ctxData.getRequestTrace());
    }

    @Nullable
    private ObjectNode postRecords(String appName, String url, Object body) {
        String appUrl = "/" + appName + url;
        return restApi.jsonPost(appUrl, body, ObjectNode.class);
    }

    @Nullable
    public RecordsDaoInfo getSourceInfo(@NotNull String sourceId) {
        //todo
        return null;
    }

    @NotNull
    public List<RecordsDaoInfo> getSourceInfo() {
        //todo
        return Collections.emptyList();
    }

    public void setDefaultAppName(String defaultAppName) {
        this.defaultAppName = defaultAppName;
    }
}
