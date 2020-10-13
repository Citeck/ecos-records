package ru.citeck.ecos.records3.rest.v1;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.delete.DelStatus;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteBody;
import ru.citeck.ecos.records3.rest.v1.delete.DeleteResp;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateBody;
import ru.citeck.ecos.records3.rest.v1.mutate.MutateResp;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.rest.v1.query.QueryResp;
import ru.citeck.ecos.records3.rest.v1.query.QueryBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RestHandlerV1 {

    private final RecordsService recordsService;
    private final RecordsServiceFactory factory;

    public RestHandlerV1(RecordsServiceFactory factory) {
        this.factory = factory;
        this.recordsService = factory.getRecordsServiceV1();
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

        Map<String, String> bodyAtts = body.getAttributes();
        if (bodyAtts == null) {
            bodyAtts = Collections.emptyMap();
        }
        Map<String, String> attributes = bodyAtts;
        QueryResp resp = new QueryResp();

        if (body.getQuery() != null) {

            // search query

            RecordsQueryRes<RecordAtts> result;
            try {
                result = doInTransaction(true, () ->
                        recordsService.query(body.getQuery(), attributes, body.isRawAtts())
                );
            } catch (Exception e) {
                log.error("Records search query exception. QueryBody: " + body, e);
                result = new RecordsQueryRes<>();
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
                                recordsService.getAtts(records, body.getAttributes(), body.isRawAtts())
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
            ctxData.setRequestTrace(body.getRequestTrace());
            ctxData.setMsgLevel(body.getMsgLevel());

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
