package ru.citeck.ecos.records3.rest.v1;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.rest.v1.query.QueryResp;
import ru.citeck.ecos.records3.rest.v1.query.QueryBody;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RestHandlerV1 {

    private final RecordsService recordsService;
    private final RecordsServiceFactory factory;
    private final RecordsProperties properties;

    private String currentAppId;

    public RestHandlerV1(RecordsServiceFactory factory) {
        this.factory = factory;
        this.recordsService = factory.getRecordsServiceV1();
        this.properties = factory.getProperties();

        if (StringUtils.isBlank(properties.getAppInstanceId())) {
            currentAppId = properties.getAppName();
        } else {
            currentAppId = properties.getAppInstanceId();
        }
        if (StringUtils.isBlank(currentAppId)) {
            currentAppId = "unknown:" + UUID.randomUUID();
        }
    }

    public QueryResp queryRecords(QueryBody body) {
        return doWithContext(body, ctx -> queryRecordsImpl(body, ctx));
    }

    private QueryResp queryRecordsImpl(QueryBody body, RequestContext context) {

        if (body.getQuery() != null && body.getRecords() != null) {
            context.addMsg(MsgLevel.WARN, () ->
                "There must be one of 'records' or 'query' field "
                + "but found both. 'records' field will be ignored");
        }

        JsonNode bodyAtts = body.getAttributes();
        Map<String, Object> bodyAttsMap;
        if (bodyAtts == null) {
            bodyAttsMap = Collections.emptyMap();
        } else {
            if (bodyAtts.isArray()) {
                bodyAttsMap = new LinkedHashMap<>();
                bodyAtts.forEach(att -> bodyAttsMap.put(att.asText(), att));
            } else if (bodyAtts.isObject()) {
                bodyAttsMap = DataValue.create(bodyAtts).asMap(String.class, Object.class);
            } else {
                throw new RuntimeException("Incorrect attributes: '" + bodyAtts + "'");
            }
        }
        QueryResp resp = new QueryResp();

        if (body.getQuery() != null) {

            // search query

            RecsQueryRes<RecordAtts> result;
            try {
                result = doInTransaction(true, () ->
                        recordsService.query(body.getQuery(), bodyAttsMap, body.isRawAtts())
                );
            } catch (Exception e) {
                log.error("Records search query exception. QueryBody: " + body, e);
                result = new RecsQueryRes<>();
                context.addMsg(MsgLevel.ERROR, () -> ErrorUtils.convertException(e));
            }
            Json.getMapper().applyData(resp, result);

        } else {

            // attributes query

            List<RecordRef> records = body.getRecords();

            if (!records.isEmpty()) {

                if (body.getAttributes() == null) {

                    resp.setRecords(records.stream()
                        .map(RecordAtts::new)
                        .collect(Collectors.toList()));

                } else {

                    List<RecordAtts> atts;
                    try {
                        atts = doInTransaction(true, () ->
                                recordsService.getAtts(records, bodyAttsMap, body.isRawAtts())
                        );
                    } catch (Exception e) {
                        log.error("Records attributes query exception. QueryBody: " + body, e);
                        atts = records.stream()
                            .map(RecordAtts::new)
                            .collect(Collectors.toList());
                        context.addMsg(MsgLevel.ERROR, () -> ErrorUtils.convertException(e));
                    }

                    resp.setRecords(atts);
                }
            }
        }

        resp.setMessages(context.getMessages());
        return resp;
    }

    public MutateResp mutateRecords(MutateBody body) {
        return doWithContext(body, ctx -> mutateRecordsImpl(body, ctx));
    }

    private MutateResp mutateRecordsImpl(MutateBody body, RequestContext context) {

        if (body.getRecords() == null || body.getRecords().isEmpty()) {
            return new MutateResp();
        }

        MutateResp resp = new MutateResp();
        try {
            doInTransaction(false, () ->
                resp.setRecords(recordsService.mutate(body.getRecords())
                                              .stream()
                                              .map(RecordAtts::new)
                                              .collect(Collectors.toList()))
            );
        } catch (Exception e) {
            log.error("Records mutation completed with error. MutateBody: " + body, e);
            resp.setRecords(body.getRecords()
                .stream()
                .map(r -> new RecordAtts(r.getId()))
                .collect(Collectors.toList()));
            context.addMsg(MsgLevel.ERROR, () -> ErrorUtils.convertException(e));
        }
        resp.setMessages(context.getMessages());
        return resp;
    }

    public DeleteResp deleteRecords(DeleteBody body) {
        return doWithContext(body, ctx -> deleteRecordsImpl(body, ctx));
    }

    private DeleteResp deleteRecordsImpl(DeleteBody body, RequestContext context) {

        if (body.getRecords() == null || body.getRecords().isEmpty()) {
            return new DeleteResp();
        }

        DeleteResp resp = new DeleteResp();
        try {
            doInTransaction(false, () ->
                resp.setStatuses(recordsService.delete(body.getRecords()))
            );
        } catch (Exception e) {
            log.error("Records deletion completed with error. DeleteBody: " + body, e);
            resp.setStatuses(body.getRecords()
                .stream()
                .map(r -> DelStatus.ERROR)
                .collect(Collectors.toList()));
            context.addMsg(MsgLevel.ERROR, () -> ErrorUtils.convertException(e));
        }
        resp.setMessages(context.getMessages());
        return resp;
    }

    private <T> T doWithContext(RequestBody body, Function<RequestContext, T> action) {

        return RequestContext.doWithCtx(factory, ctxData -> {

            ctxData.setRequestId(body.getRequestId());
            ctxData.setMsgLevel(body.getMsgLevel());
            List<String> trace = new ArrayList<>(body.getRequestTrace());
            trace.add(currentAppId);
            ctxData.setRequestTrace(trace);

        }, action);
    }

    private void doInTransaction(boolean readOnly, Runnable action) {
        doInTransaction(readOnly, () -> {
            action.run();
            return null;
        });
    }

    private <T> T doInTransaction(boolean readOnly, Supplier<T> action) {
        // todo
        return action.get();
    }

    public RecordsServiceFactory getFactory() {
        return factory;
    }
}
