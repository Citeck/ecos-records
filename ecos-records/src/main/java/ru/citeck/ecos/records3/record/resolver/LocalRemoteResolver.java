package ru.citeck.ecos.records3.record.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsProperties;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.read.AttSchemaReader;
import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttContext;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo;
import ru.citeck.ecos.records2.utils.RecordsUtils;
import ru.citeck.ecos.records2.utils.ValWithIdx;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalRemoteResolver {

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
    public RecsQueryRes<RecordAtts> query(@NotNull RecordsQuery query,
                                          @NotNull Map<String, ?> attributes,
                                          boolean rawAtts) {

        String sourceId = query.getSourceId();

        if (remote == null || !isRemoteSourceId(sourceId)) {
            return doWithSchema(attributes, schema -> local.query(query, schema, rawAtts));
        }
        return remote.query(query, attributes, rawAtts);
    }

    private <T> T doWithSchema(Map<String, ?> attributes, Function<List<SchemaAtt>, T> action) {

        List<SchemaAtt> atts = reader.read(attributes);
        return AttContext.doInContext(serviceFactory, attContext -> {

            if (!atts.isEmpty()) {
                attContext.setSchemaAtt(SchemaAtt.create()
                    .setName("")
                    .setInner(atts)
                    .build());
            }

            return action.apply(atts);
        });
    }

    @Nullable
    public List<RecordAtts> getAtts(@NotNull List<?> records,
                                    @NotNull Map<String, ?> attributes,
                                    boolean rawAtts) {

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        if (remote == null) {
            return doWithSchema(attributes, atts -> local.getAtts(records, atts, rawAtts));
        }

        RequestContext context = RequestContext.getCurrentNotNull();

        List<ValWithIdx<Object>> recordObjs = new ArrayList<>();
        List<ValWithIdx<RecordRef>> recordRefs = new ArrayList<>();

        int idx = 0;
        for (Object rec : records) {
            if (rec instanceof RecordRef) {
                recordRefs.add(new ValWithIdx<>((RecordRef) rec, idx));
            } else {
                recordObjs.add(new ValWithIdx<>(rec, idx));
            }
            idx++;
        }

        List<ValWithIdx<RecordAtts>> results = new ArrayList<>();

        List<Object> recordsObjValue = recordObjs.stream()
            .map(ValWithIdx::getValue)
            .collect(Collectors.toList());

        List<RecordAtts> objAtts = doWithSchema(attributes, atts -> local.getAtts(recordsObjValue, atts, rawAtts));

        if (objAtts != null && objAtts.size() == recordsObjValue.size()) {
            for (int i = 0; i < objAtts.size(); i++) {
                results.add(new ValWithIdx<>(objAtts.get(i), recordObjs.get(i).getIdx()));
            }
        } else {
            context.addMsg(MsgLevel.ERROR, () -> "Results count doesn't match with " +
                "requested. objAtts: " + objAtts + " recordsObjValue: " + recordsObjValue);
            return null;
        }

        RecordsUtils.groupRefBySourceWithIdx(recordRefs).forEach((sourceId, recs) -> {

            List<RecordRef> refs = recs.stream()
                .map(ValWithIdx::getValue)
                .collect(Collectors.toList());

            List<RecordAtts> atts;

            if (!isRemoteSourceId(sourceId)) {

                atts = doWithSchema(attributes, schema -> local.getAtts(refs, schema, rawAtts));

            } else if (isRemoteRef(recs.stream()
                            .map(ValWithIdx::getValue)
                            .findFirst()
                            .orElse(null))) {

                atts = remote.getAtts(refs, attributes, rawAtts);

            } else {

                atts = doWithSchema(attributes, schema -> local.getAtts(refs, schema, rawAtts));
            }

            if (atts == null || atts.size() != refs.size()) {

                context.addMsg(MsgLevel.ERROR, () -> "Results count doesn't match with " +
                    "requested. Atts: " + atts + " refs: " + refs);

                for (ValWithIdx<RecordRef> record : recs) {
                    results.add(new ValWithIdx<>(new RecordAtts(record.getValue()), record.getIdx()));
                }
            } else {
                for (int i = 0; i < refs.size(); i++) {
                    results.add(new ValWithIdx<>(atts.get(i), recs.get(i).getIdx()));
                }
            }
        });

        results.sort(Comparator.comparingInt(ValWithIdx::getIdx));
        return results.stream().map(ValWithIdx::getValue).collect(Collectors.toList());
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
    public RecordsDaoInfo getSourceInfo(@NotNull String sourceId) {
        if (isRemoteSourceId(sourceId)) {
            return remote.getSourceInfo(sourceId);
        }
        return local.getSourceInfo(sourceId);
    }

    @NotNull
    public List<RecordsDaoInfo> getSourceInfo() {
        List<RecordsDaoInfo> result = new ArrayList<>(local.getSourceInfo());
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

    public void register(String sourceId, RecordsDao recordsDao) {
        local.register(sourceId, recordsDao);
    }
}
