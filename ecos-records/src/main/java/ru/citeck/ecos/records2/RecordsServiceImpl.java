package ru.citeck.ecos.records2;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import ecos.com.fasterxml.jackson210.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.graphql.GqlConstants;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.DebugResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.resolver.LocalRecordsResolverV0;
import ru.citeck.ecos.records2.source.dao.RecordsDao;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.atts.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.atts.schema.read.DtoSchemaReader;
import ru.citeck.ecos.records3.record.atts.schema.write.AttSchemaWriter;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.record.request.msg.ReqMsg;
import ru.citeck.ecos.records3.utils.V1ConvUtils;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @deprecated use RecordsService from records3 package
 */
@Slf4j
@Deprecated
public class RecordsServiceImpl extends AbstractRecordsService implements ServiceFactoryAware {

    private RecordsService recordsServiceV1;
    private LocalRecordsResolverV0 localRecordsResolverV0;
    private AttSchemaReader attSchemaReader;
    private AttSchemaWriter attSchemaWriter;
    private DtoSchemaReader dtoSchemaReader;

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
    }

    /* QUERY */

    @NotNull
    @Override
    public RecordsQueryResult<EntityRef> queryRecords(RecordsQuery query) {
        return handleRecordsQuery(() -> {

            ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery queryV1 =
                Json.getMapper().convert(query, ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery.class);
            if (queryV1 == null) {
                return new RecordsQueryResult<>();
            }

            RequestContext context = RequestContext.getCurrentNotNull();

            RecsQueryRes<EntityRef> queryRes = recordsServiceV1.query(queryV1);

            RecordsQueryResult<EntityRef> result = new RecordsQueryResult<>();
            result.setRecords(queryRes.getRecords());
            result.setTotalCount(queryRes.getTotalCount());
            result.setHasMore(queryRes.getHasMore());
            result.setErrors(context.getRecordErrors());
            setDebugToResult(result, context);

            return result;
        }, () -> "Query: " + query);
    }

    private void setDebugToResult(DebugResult result, RequestContext context) {
        List<ReqMsg> messages = context.getMessages();
        if (!messages.isEmpty()) {
            ObjectData debug = ObjectData.create();
            debug.set("messages", messages);
            result.setDebug(debug);
        }
    }

    @NotNull
    @Override
    public <T> RecordsQueryResult<T> queryRecords(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = attSchemaWriter.writeToMap(dtoSchemaReader.read(metaClass));
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }

        RecordsQueryResult<RecordMeta> meta = queryRecords(query, attributes);

        return new RecordsQueryResult<>(meta, m -> dtoSchemaReader.instantiate(metaClass, m.getAttributes()));
    }

    @NotNull
    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, Map<String, String> attributes) {
        return queryRecords(query, attributes, true);
    }

    @NotNull
    private RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query,
                                                        Map<String, String> attributes,
                                                        boolean flatAttributes) {

        return RequestContext.doWithCtx(serviceFactory, ctx -> {

            List<SchemaAtt> attsWithFixedAliases = attSchemaReader.read(attributes);

            RecsQueryRes<RecordAtts> result = recordsServiceV1.query(
                V1ConvUtils.recsQueryV0ToV1(query),
                attSchemaWriter.writeToMap(attsWithFixedAliases),
                !flatAttributes
            );

            RecordsQueryResult<RecordMeta> metaResult = new RecordsQueryResult<>();

            metaResult.setRecords(result.getRecords()
                .stream()
                .map(RecordMeta::new)
                .map(meta -> {
                    if (!flatAttributes) {
                        return fixRawData(attsWithFixedAliases, meta);
                    } else {
                        return meta;
                    }
                })
                .collect(Collectors.toList()));

            metaResult.setHasMore(result.getHasMore());
            metaResult.setTotalCount(result.getTotalCount());

            metaResult.setErrors(ctx.getRecordErrors());
            List<ReqMsg> debugMsgs = ctx.getMessages()
                .stream()
                .filter(m -> MsgLevel.DEBUG.isEnabled(m.getLevel()))
                .collect(Collectors.toList());

            if (!debugMsgs.isEmpty()) {
                ObjectData debugData = ObjectData.create();
                debugData.set("messages", debugMsgs);
                metaResult.setDebug(debugData);
            }

            return metaResult;
        });
    }

    /* ATTRIBUTES */

    @NotNull
    @Override
    public DataValue getAtt(EntityRef record, String attribute) {
        return getAttribute(record, attribute);
    }

    @NotNull
    @Override
    public DataValue getAttribute(EntityRef record, String attribute) {

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
    public RecordsResult<RecordMeta> getAttributes(Collection<EntityRef> records,
                                                   Map<String, String> attributes) {

        return getAttributesImpl(records, attributes, true);
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getRawAttributes(Collection<EntityRef> records, Map<String, String> attributes) {
        return getAttributesImpl(records, attributes, false);
    }

    @NotNull
    private RecordsResult<RecordMeta> getAttributesImpl(Collection<EntityRef> records,
                                                        Map<String, String> attributes,
                                                        boolean flatAttributes) {

        if (attributes.isEmpty()) {
            return new RecordsResult<>(new ArrayList<>(records), RecordMeta::new);
        }
        RecordsResult<RecordMeta> recsResult = new RecordsResult<>();

        recsResult.setRecords(RequestContext.doWithCtx(serviceFactory, ctx -> {

            List<SchemaAtt> attsWithFixedAliases = attSchemaReader.read(attributes);
            List<RecordAtts> atts = recordsServiceV1.getAtts(
                records,
                attSchemaWriter.writeToMap(attsWithFixedAliases),
                !flatAttributes
            );

            List<RecordMeta> attsMeta = Json.getMapper().convert(atts, Json.getMapper().getListType(RecordMeta.class));

            if (attsMeta != null && !flatAttributes) {
                attsMeta = attsMeta.stream()
                    .map(r -> fixRawData(attsWithFixedAliases, r))
                    .collect(Collectors.toList());
            }

            setDebugToResult(recsResult, ctx);
            recsResult.setErrors(ctx.getRecordErrors());

            return attsMeta;
        }));

        return recsResult;
    }

    /* META */

    @NotNull
    @Override
    public <T> T getMeta(@NotNull EntityRef recordRef, @NotNull Class<T> metaClass) {

        RecordsResult<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.getRecords().size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Ref: " + recordRef + " Result: " + meta);
        }
        return meta.getRecords().get(0);
    }

    @NotNull
    @Override
    public <T> RecordsResult<T> getMeta(@NotNull Collection<EntityRef> records, @NotNull Class<T> metaClass) {

        Map<String, String> attributes = attSchemaWriter.writeToMap(dtoSchemaReader.read(metaClass));
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> dtoSchemaReader.instantiate(metaClass, m.getAttributes()));
    }

    private RecordMeta fixRawData(List<SchemaAtt> attsSchema, RecordMeta meta) {

        ObjectData attributes = meta.getAttributes();
        for (SchemaAtt att : attsSchema) {
            attributes.set(att.getAliasForValue(),
                fixRawData(null, att, attributes.get(att.getAliasForValue()).asJson(), att.getMultiple()));
        }
        meta.setAtts(attributes);

        return meta;
    }

    private JsonNode fixRawData(@Nullable SchemaAtt parent,
                                SchemaAtt att,
                                @Nullable JsonNode node,
                                boolean isMultiple) {

        if (node == null || node.isNull() || node.isMissingNode() || att.isScalar()) {
            return node;
        }

        if (node.isArray() && isMultiple) {
            ArrayNode result = Json.getMapper().newArrayNode();
            node.forEach(n -> result.add(fixRawData(parent, att, n, false)));
            return result;
        }

        String name = att.getName();

        if ((name.equals(RecordConstants.ATT_HAS)
            || name.equals(RecordConstants.ATT_AS)
            || name.equals(RecordConstants.ATT_EDGE)) && att.getInner().size() == 1) {

            List<SchemaAtt> innerAtts = att.getInner().get(0).getInner();

            if (name.equals(RecordConstants.ATT_HAS)) {
                return node.elements().next().elements().next();
            }

            ObjectNode newNode = Json.getMapper().newObjectNode();
            JsonNode innerElements = node.elements().next();

            for (SchemaAtt innerAtt : innerAtts) {

                JsonNode newInnerNode = innerElements.get(innerAtt.getAliasForValue());

                newNode.put(innerAtt.getAliasForValue(), fixRawData(
                    att,
                    innerAtt,
                    newInnerNode,
                    innerAtt.getMultiple()));
            }
            return newNode;
        }

        if (parent != null && parent.getName().equals(RecordConstants.ATT_EDGE)) {
            if (!GqlConstants.META_VAL_FIELDS.contains(att.getName())) {
                SchemaAtt newAtt = att.getInner().get(0);
                if (newAtt.isScalar()) {
                    return node.elements().next();
                }
            }
        }

        if (att.isScalar()) {
            return node;
        }

        ObjectNode result = Json.getMapper().newObjectNode();
        for (SchemaAtt innerAtt : att.getInner()) {
            result.put(innerAtt.getAliasForValue(),
                fixRawData(att, innerAtt, node.get(innerAtt.getAliasForValue()), innerAtt.getMultiple()));
        }
        return result;
    }

    /* MODIFICATION */

    @NotNull
    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        List<RecordAtts> records = Json.getMapper().convert(mutation.getRecords(),
            Json.getMapper().getListType(RecordAtts.class));

        if (records == null) {

            List<RecordMeta> resRecords = mutation.getRecords()
                .stream()
                .map(r -> new RecordMeta(r.getId()))
                .collect(Collectors.toList());

            RecordsMutResult res = new RecordsMutResult();
            res.setRecords(resRecords);

            return res;
        }

        List<EntityRef> mutateV1Res = recordsServiceV1.mutate(records);
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

    private <T> RecordsQueryResult<T> handleRecordsQuery(Supplier<RecordsQueryResult<T>> supplier,
                                                         Supplier<String> logInfo) {
        return handleRecordsRead(supplier, RecordsQueryResult::new, logInfo);
    }

    private <T extends RecordsResult> T handleRecordsRead(Supplier<T> impl,
                                                          Supplier<T> orElse,
                                                          Supplier<String> logInfo) {

        T result;

        try {
            result = QueryContext.withContext(serviceFactory, impl);
        } catch (Throwable e) {
            String logMsg = "Records resolving error.";
            if (logInfo != null) {
                logMsg += " " + logInfo.get();
            }
            log.error(logMsg, e);
            result = orElse.get();
            result.addError(ErrorUtils.convertException(e, serviceFactory));
        }

        return result;
    }

    @Override
    public void register(@NotNull RecordsDao recordsDao) {
        register(recordsDao.getId(), recordsDao);
    }

    @Override
    public void register(@NotNull String sourceId, @NotNull RecordsDao recordsDao) {
        localRecordsResolverV0.register(recordsDao.getId(), recordsDao);
    }

    @Override
    public void unregister(String sourceId) {
        localRecordsResolverV0.unregister(sourceId);
    }

    @Override
    public void setRecordsServiceFactory(@NotNull RecordsServiceFactory serviceFactory) {

        dtoSchemaReader = serviceFactory.getDtoSchemaReader();
        recordsServiceV1 = serviceFactory.getRecordsServiceV1();
        localRecordsResolverV0 = serviceFactory.getLocalRecordsResolverV0();
        attSchemaWriter = serviceFactory.getAttSchemaWriter();
        attSchemaReader = serviceFactory.getAttSchemaReader();

        for (Object dao : serviceFactory.getDefaultRecordsDao()) {
            if (dao instanceof RecordsDao) {
                register((RecordsDao) dao);
            }
        }
    }
}
