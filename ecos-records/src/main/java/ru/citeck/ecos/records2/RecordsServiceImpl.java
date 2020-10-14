package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.JsonMapper;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.meta.AttributesSchema;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.DebugResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.op.atts.RecordAtts;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.op.query.RecordsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RecordsServiceImpl extends AbstractRecordsService {

    private final RecordsService recordsServiceV1;
    private final RecordsMetaService recordsMetaService;
    private final RecordsMetaGql recordsMetaGql;

    private final JsonMapper mapper = Json.getMapper();

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        recordsServiceV1 = serviceFactory.getRecordsServiceV1();
        recordsMetaService = serviceFactory.getRecordsMetaService();
        recordsMetaGql = serviceFactory.getRecordsMetaGql();
    }

    /* QUERY */

    @NotNull
    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {
        return handleRecordsQuery(() -> {

            ru.citeck.ecos.records3.record.op.query.RecordsQuery queryV1 =
                mapper.convert(query, ru.citeck.ecos.records3.record.op.query.RecordsQuery.class);
            if (queryV1 == null) {
                return new RecordsQueryResult<>();
            }

            RequestContext context = RequestContext.getCurrentNotNull();

            RecordsQueryRes<RecordRef> queryRes = recordsServiceV1.query(queryV1);

            RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();
            result.setRecords(queryRes.getRecords());
            result.setTotalCount(queryRes.getTotalCount());
            result.setHasMore(queryRes.isHasMore());
            result.setErrors(context.getRecordErrors());
            setDebugToResult(result, context);

            return result;
        });
    }

    private void setDebugToResult(DebugResult result, RequestContext context) {
        List<RequestMsg> messages = context.getMessages();
        if (!messages.isEmpty()) {
            ObjectData debug = ObjectData.create();
            debug.set("messages", messages);
            result.setDebug(debug);
        }
    }

    @NotNull
    @Override
    public <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }

        RecordsQueryResult<RecordMeta> meta = queryRecords(query, attributes);

        return new RecordsQueryResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @NotNull
    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {

        AttributesSchema schema = recordsMetaService.createSchema(attributes);
        RecordsQueryResult<RecordMeta> records = queryRecords(query, schema.getSchema());
        records.setRecords(recordsMetaService.convertMetaResult(records.getRecords(), schema, true));

        return records;
    }

    @NotNull
    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {

        MetaField metaField = recordsMetaGql.getMetaFieldFromSchema(schema);
        Map<String, String> attributes = metaField.getInnerAttributesMap();

        return queryRecords(query, attributes);
    }

    /* ATTRIBUTES */

    @NotNull
    @Override
    public DataValue getAtt(RecordRef record, String attribute) {
        return getAttribute(record, attribute);
    }

    @NotNull
    @Override
    public DataValue getAttribute(RecordRef record, String attribute) {

        if (record == null) {
            return DataValue.create(null);
        }

        RecordsResult<RecordMeta> meta = getAttributes(Collections.singletonList(record),
                                                       Collections.singletonList(attribute));
        if (!meta.getRecords().isEmpty()) {
            return meta.getRecords().get(0).getAttribute(attribute);
        }
        return DataValue.create(null);
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getAttributes(Collection<RecordRef> records,
                                                   Map<String, String> attributes) {

        return getAttributesImpl(records, attributes, true);
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getRawAttributes(Collection<RecordRef> records, Map<String, String> attributes) {
        return getAttributesImpl(records, attributes, false);
    }

    @NotNull
    private RecordsResult<RecordMeta> getAttributesImpl(Collection<RecordRef> records,
                                                        Map<String, String> attributes,
                                                        boolean flatAttributes) {

        if (attributes.isEmpty()) {
            return new RecordsResult<>(new ArrayList<>(records), RecordMeta::new);
        }
        RecordsResult<RecordMeta> recsResult = new RecordsResult<>();

        recsResult.setRecords(RequestContext.doWithCtx(serviceFactory, ctx -> {

            List<RecordAtts> atts = recordsServiceV1.getAtts(records, attributes, !flatAttributes);
            List<RecordMeta> attsMeta = mapper.convert(atts, mapper.getListType(RecordMeta.class));

            setDebugToResult(recsResult, ctx);
            recsResult.setErrors(ctx.getRecordErrors());

            return attsMeta;
        }));

        return recsResult;
    }

    /* META */

    @NotNull
    @Override
    public <T> T getMeta(@NotNull RecordRef recordRef, @NotNull Class<T> metaClass) {

        RecordsResult<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.getRecords().size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Ref: " + recordRef + " Result: " + meta);
        }
        return meta.getRecords().get(0);
    }

    @NotNull
    @Override
    public <T> RecordsResult<T> getMeta(@NotNull Collection<RecordRef> records, @NotNull Class<T> metaClass) {

        Map<String, String> attributes = recordsMetaService.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {

        MetaField metaField = recordsMetaGql.getMetaFieldFromSchema(schema);
        Map<String, String> attributes = metaField.getInnerAttributesMap();

        return getAttributes(records, attributes);
    }

    /* MODIFICATION */

    @NotNull
    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        List<RecordAtts> records = mapper.convert(mutation.getRecords(),
            mapper.getListType(RecordAtts.class));

        if (records == null) {

            List<RecordMeta> resRecords = mutation.getRecords()
                .stream()
                .map(r -> new RecordMeta(r.getId()))
                .collect(Collectors.toList());

            RecordsMutResult res = new RecordsMutResult();
            res.setRecords(resRecords);

            return res;
        }

        List<RecordRef> mutateV1Res = recordsServiceV1.mutate(records);
        RecordsMutResult mutResult = new RecordsMutResult();
        mutResult.setRecords(mutateV1Res.stream()
            .map(RecordMeta::new)
            .collect(Collectors.toList()));

        return mutResult;
    }

    @NotNull
    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {

        recordsServiceV1.delete(deletion.getRecords());

        List<RecordMeta> resultMeta = deletion.getRecords()
            .stream()
            .map(RecordMeta::new)
            .collect(Collectors.toList());

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultMeta);

        return result;
    }

    /* OTHER */

    private <T> RecordsQueryResult<T> handleRecordsQuery(Supplier<RecordsQueryResult<T>> supplier) {
        return handleRecordsRead(supplier, RecordsQueryResult::new);
    }

    private <T extends RecordsResult> T handleRecordsRead(Supplier<T> impl, Supplier<T> orElse) {

        T result;

        try {
            result = QueryContext.withContext(serviceFactory, impl);
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = orElse.get();
            result.addError(ErrorUtils.convertException(e));
        }

        return result;
    }
}
