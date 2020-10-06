package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.record.operation.delete.DelStatus;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.operation.meta.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;
import ru.citeck.ecos.records3.utils.RecordsUtils;
import ru.citeck.ecos.records3.utils.ValueWithIdx;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalRemoteResolver implements RecordsDaoRegistry {

    private final LocalRecordsResolver local;
    private final RemoteRecordsResolver remote;
    private final AttSchemaReader reader;
    private final RecordsServiceFactory serviceFactory;

    private final String currentAppSourceIdPrefix;

    public LocalRemoteResolver(RecordsServiceFactory serviceFactory) {

        this.serviceFactory = serviceFactory;
        this.remote = serviceFactory.getRemoteRecordsResolver();
        this.local = serviceFactory.getLocalRecordsResolver();
        this.reader = serviceFactory.getAttSchemaReader();

        RecordsProperties props = serviceFactory.getProperties();
        this.currentAppSourceIdPrefix = props.getAppName() + "/";

        MandatoryParam.check("local", local);
    }

    @Nullable
    public RecordsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                             @NotNull Map<String, String> attributes,
                                             boolean rawAtts) {

        String sourceId = query.getSourceId();

        if (remote == null || !isRemoteSourceId(sourceId)) {
            return doWithSchema(attributes, schema -> local.query(query, schema, rawAtts));
        }
        return remote.query(query, attributes, rawAtts);
    }

    private <T> T doWithSchema(Map<String, String> attributes, Function<List<SchemaRootAtt>, T> action) {

        List<SchemaRootAtt> atts = reader.readRoot(attributes);
        return AttContext.doInContext(serviceFactory, attContext -> {

            if (!atts.isEmpty()) {
                attContext.setSchemaAtt(SchemaAtt.create()
                    .setName("")
                    .setInner(atts.stream()
                        .map(SchemaRootAtt::getAttribute)
                        .collect(Collectors.toList()))
                    .build());
            }

            return action.apply(atts);
        });
    }

    @Nullable
    public List<RecordAtts> getAtts(@NotNull List<?> records,
                                    @NotNull Map<String, String> attributes,
                                    boolean rawAtts) {

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        if (remote == null) {
            return doWithSchema(attributes, atts -> local.getAtts(records, atts, rawAtts));
        }

        RequestContext context = RequestContext.getCurrentNotNull();

        List<ValueWithIdx<Object>> recordObjs = new ArrayList<>();
        List<ValueWithIdx<RecordRef>> recordRefs = new ArrayList<>();

        int idx = 0;
        for (Object rec : records) {
            if (rec instanceof RecordRef) {
                recordRefs.add(new ValueWithIdx<>((RecordRef) rec, idx));
            } else {
                recordObjs.add(new ValueWithIdx<>(rec, idx));
            }
            idx++;
        }

        List<ValueWithIdx<RecordAtts>> results = new ArrayList<>();

        List<Object> recordsObjValue = recordObjs.stream()
            .map(ValueWithIdx::getValue)
            .collect(Collectors.toList());

        List<RecordAtts> objAtts = doWithSchema(attributes, atts -> local.getAtts(recordsObjValue, atts, rawAtts));

        if (objAtts != null && objAtts.size() == recordsObjValue.size()) {
            for (int i = 0; i < objAtts.size(); i++) {
                results.add(new ValueWithIdx<>(objAtts.get(i), recordObjs.get(i).getIdx()));
            }
        } else {
            context.addMsg(MsgLevel.ERROR, () -> "Results count doesn't match with " +
                "requested. objAtts: " + objAtts + " recordsObjValue: " + recordsObjValue);
            return null;
        }

        RecordsUtils.groupRefBySourceWithIdx(recordRefs).forEach((sourceId, recs) -> {

            List<RecordRef> refs = recs.stream()
                .map(ValueWithIdx::getValue)
                .collect(Collectors.toList());

            List<RecordAtts> atts;

            if (!isRemoteSourceId(sourceId)) {

                atts = doWithSchema(attributes, schema -> local.getAtts(refs, schema, rawAtts));

            } else if (isRemoteRef(recs.stream()
                            .map(ValueWithIdx::getValue)
                            .findFirst()
                            .orElse(null))) {

                atts = remote.getAtts(refs, attributes, rawAtts);

            } else {

                atts = doWithSchema(attributes, schema -> local.getAtts(refs, schema, rawAtts));
            }

            if (atts == null || atts.size() != refs.size()) {

                context.addMsg(MsgLevel.ERROR, () -> "Results count doesn't match with " +
                    "requested. Atts: " + atts + " refs: " + refs);

                for (ValueWithIdx<RecordRef> record : recs) {
                    results.add(new ValueWithIdx<>(new RecordAtts(record.getValue()), record.getIdx()));
                }
            } else {
                for (int i = 0; i < refs.size(); i++) {
                    results.add(new ValueWithIdx<>(atts.get(i), recs.get(i).getIdx()));
                }
            }
        });

        results.sort(Comparator.comparingInt(ValueWithIdx::getIdx));
        return results.stream().map(ValueWithIdx::getValue).collect(Collectors.toList());
    }

    @Nullable
    public List<RecordRef> mutate(@NotNull List<RecordAtts> records) {
        if (remote == null || records.isEmpty()) {
            return local.mutate(records);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.mutate(records);
        }
        return local.mutate(records);
    }

    @Nullable
    public List<DelStatus> delete(@NotNull List<RecordRef> records) {
        if (remote == null || records.isEmpty()) {
            return local.delete(records);
        }
        if (isRemoteRef(records.get(0))) {
            return remote.delete(records);
        }
        return local.delete(records);
    }

    @Nullable
    public RecsSourceInfo getSourceInfo(@NotNull String sourceId) {
        if (isRemoteSourceId(sourceId)) {
            return remote.getSourceInfo(sourceId);
        }
        return local.getSourceInfo(sourceId);
    }

    @NotNull
    public List<RecsSourceInfo> getSourceInfo() {
        List<RecsSourceInfo> result = new ArrayList<>(local.getSourceInfo());
        result.addAll(remote.getSourceInfo());
        return result;
    }

    private boolean isRemoteRef(RecordAtts meta) {
        return isRemoteRef(meta.getId());
    }

    private boolean isRemoteRef(RecordRef ref) {
        return ref != null && ref.isRemote() && isRemoteSourceId(ref.getAppName() + "/" + ref.getSourceId());
    }

    private boolean isRemoteSourceId(String sourceId) {
        if (StringUtils.isBlank(sourceId)) {
            return false;
        }
        if (local.containsDao(sourceId)) {
            return false;
        }
        return sourceId.contains("/") && !sourceId.startsWith(currentAppSourceIdPrefix);
    }

    @Override
    public void register(String sourceId, RecordsDao recordsDao) {
        local.register(sourceId, recordsDao);
    }
}
