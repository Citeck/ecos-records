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
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
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

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @deprecated use RecordsService from records3 package
 */
@Slf4j
@Deprecated
public class RecordsServiceImpl extends AbstractRecordsService {

    private final RecordsService recordsServiceV1;
    private final RecordsMetaGql recordsMetaGql;
    private final LocalRecordsResolverV0 localRecordsResolverV0;
    private final AttSchemaReader attSchemaReader;
    private final AttSchemaWriter attSchemaWriter;
    private final DtoSchemaReader dtoSchemaReader;

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        dtoSchemaReader = serviceFactory.getDtoSchemaReader();
        recordsServiceV1 = serviceFactory.getRecordsServiceV1();
        recordsMetaGql = serviceFactory.getRecordsMetaGql();
        localRecordsResolverV0 = serviceFactory.getLocalRecordsResolverV0();
        attSchemaWriter = serviceFactory.getAttSchemaWriter();
        attSchemaReader = serviceFactory.getAttSchemaReader();
    }

    /* QUERY */

    @NotNull
    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {
        return handleRecordsQuery(() -> {

            ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery queryV1 =
                Json.getMapper().convert(query, ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery.class);
            if (queryV1 == null) {
                return new RecordsQueryResult<>();
            }

            RequestContext context = RequestContext.getCurrentNotNull();

            RecsQueryRes<RecordRef> queryRes = recordsServiceV1.query(queryV1);

            RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();
            result.setRecords(queryRes.getRecords());
            result.setTotalCount(queryRes.getTotalCount());
            result.setHasMore(queryRes.getHasMore());
            result.setErrors(context.getRecordErrors());
            setDebugToResult(result, context);

            return result;
        });
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

            List<SchemaAtt> attsWithFixedAliases = fixInnerAliases(attributes);

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

    @NotNull
    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String schema) {
        Map<String, String> attsToLoad = convertSchemaToAtts(schema);
        return queryRecords(query, attsToLoad, false);
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

            List<SchemaAtt> attsWithFixedAliases = fixInnerAliases(attributes);
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

        Map<String, String> attributes = attSchemaWriter.writeToMap(dtoSchemaReader.read(metaClass));
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        RecordsResult<RecordMeta> meta = getAttributes(records, attributes);

        return new RecordsResult<>(meta, m -> dtoSchemaReader.instantiate(metaClass, m.getAttributes()));
    }

    @NotNull
    @Override
    public RecordsResult<RecordMeta> getMeta(Collection<RecordRef> records, String schema) {
        return getAttributesImpl(records, convertSchemaToAtts(schema), false);
    }

    private Map<String, String> convertSchemaToAtts(String schema) {

        MetaField metaField = recordsMetaGql.getMetaFieldFromSchema(schema);
        Map<String, String> attributes = metaField.getInnerAttributesMap(true);
        Map<String, String> fixedAtts = new HashMap<>();

        attributes.forEach((k, v) -> fixedAtts.put(k.charAt(0) == '.' ? k.substring(1) : k, v));

        return fixedAtts;
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

    private List<SchemaAtt> fixInnerAliases(Map<String, String> attributes) {
        Map<String, String> atts = new LinkedHashMap<>();
        attributes.forEach((k, v) -> {
            if (v.charAt(0) != '.') {
                atts.put(k, v.replace("\\\"", "\""));
            } else {
                atts.put(k, v);
            }
        });
        List<SchemaAtt> schemaAtts = attSchemaReader.read(atts);
        return fixInnerAliases(schemaAtts, true, null, null);
    }

    private List<SchemaAtt> fixInnerAliases(List<SchemaAtt> atts,
                                            boolean root,
                                            @Nullable SchemaAtt parent,
                                            @Nullable SchemaAtt parentParent) {
        if (atts.isEmpty()) {
            return atts;
        }
        return atts.stream()
            .map(a -> {
                String newAlias;
                if (root) {
                    newAlias = a.getAlias();
                } else {
                    newAlias = fixInnerAlias(a.getAliasForValue());
                    if (a.getAlias().isEmpty()) {
                        if (a.getName().charAt(0) != '?') {
                            switch (a.getName()) {
                                case RecordConstants.ATT_AS:
                                    newAlias = "as";
                                    break;
                                case RecordConstants.ATT_EDGE:
                                    newAlias = "edge";
                                    break;
                                case RecordConstants.ATT_HAS:
                                    newAlias = "has";
                                    break;
                                default:
                                    if (parentParent != null
                                            && parentParent.getName().equals(RecordConstants.ATT_EDGE)) {
                                        newAlias = a.getName();
                                    } else {
                                        newAlias = a.getMultiple() ? "atts" : "att";
                                    }
                            }
                        }
                    }
                }
                return a.copy()
                    .withInner(fixInnerAliases(a.getInner(), false, a, parent))
                    .withAlias(newAlias)
                    .build();
            }).collect(Collectors.toList());
    }

    private String fixInnerAlias(String alias) {
        int roundBraceIdx = alias.indexOf('(');
        int curveBraceIdx = alias.indexOf('{');
        int minBraceIdx;
        if (roundBraceIdx == -1) {
            minBraceIdx = curveBraceIdx;
        } else if (curveBraceIdx == -1) {
            minBraceIdx = roundBraceIdx;
        } else {
            minBraceIdx = Math.min(roundBraceIdx, curveBraceIdx);
        }
        alias = minBraceIdx == -1 ? alias : alias.substring(0, minBraceIdx);
        return alias.charAt(0) == '?' ? alias.substring(1) : alias;
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

    @Override
    public void register(@NotNull RecordsDao recordsDao) {
        register(recordsDao.getId(), recordsDao);
    }

    @Override
    public void register(@NotNull String sourceId, @NotNull RecordsDao recordsDao) {
        localRecordsResolverV0.register(recordsDao.getId(), recordsDao);
    }
}
