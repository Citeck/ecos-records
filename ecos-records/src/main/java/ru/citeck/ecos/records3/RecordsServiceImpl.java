package ru.citeck.ecos.records3;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.record.operation.meta.RecordsMetaService;
import ru.citeck.ecos.records3.record.operation.meta.schema.read.DtoSchemaResolver;
import ru.citeck.ecos.records3.record.operation.query.QueryContext;
import ru.citeck.ecos.records3.record.operation.delete.request.RecordsDelResult;
import ru.citeck.ecos.records3.record.operation.delete.request.RecordsDeletion;
import ru.citeck.ecos.records3.record.error.ErrorUtils;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutation;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.resolver.RecordsDaoRegistry;
import ru.citeck.ecos.records3.record.resolver.RecordsResolver;
import ru.citeck.ecos.records3.source.dao.RecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class RecordsServiceImpl extends AbstractRecordsService {

    private static final Pattern ATT_PATTERN = Pattern.compile("^\\.atts?\\((n:)?[\"']([^\"']+)[\"']\\).+");

    private final RecordsResolver recordsResolver;
    private final RecordsMetaService recordsMetaService;

    private final DtoSchemaResolver dtoAttributesResolver;

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        dtoAttributesResolver = serviceFactory.getDtoMetaResolver();
        recordsResolver = serviceFactory.getRecordsResolver();
        recordsMetaService = serviceFactory.getRecordsMetaService();
    }

    /* QUERY */

    @NotNull
    @Override
    public RecsQueryRes<RecordRef> queryRecords(RecordsQuery query) {
        return handleRecordsQuery(() -> {
            RecsQueryRes<RecordMeta> metaResult = recordsResolver.queryRecords(query,
                Collections.emptyMap(), true);
            return new RecsQueryRes<>(metaResult, RecordMeta::getId);
        });
    }

    @NotNull
    @Override
    public <T> RecsQueryRes<T> queryRecords(RecordsQuery query, Class<T> metaClass) {

        Map<String, String> attributes = dtoAttributesResolver.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Meta class doesn't has any fields with setter. Class: " + metaClass);
        }

        RecsQueryRes<RecordMeta> meta = queryRecords(query, attributes);

        return new RecsQueryRes<>(meta, m -> recordsMetaService.instantiateMeta(metaClass, m));
    }

    @NotNull
    @Override
    public RecsQueryRes<RecordMeta> queryRecords(RecordsQuery query,
                                                 Map<String, String> attributes,
                                                 boolean rawAtts) {

        RecsQueryRes<RecordMeta> result = handleRecordsQuery(() ->
            recordsResolver.queryRecords(query, attributes, true));

        if (result == null) {
            result = new RecsQueryRes<>();
        }
        return result;
    }

    /* ATTRIBUTES */


    @NotNull
    @Override
    public List<RecordMeta> getAtts(Collection<RecordRef> records,
                                    Map<String, String> attributes,
                                    boolean rawAtts) {

        return getAttsImpl(records, attributes, rawAtts);
    }

    @NotNull
    private List<RecordMeta> getAttsImpl(Collection<RecordRef> records,
                                         Map<String, String> attributes,
                                         boolean rawAtts) {

        if (attributes.isEmpty()) {
            return records.stream().map(RecordMeta::new).collect(Collectors.toList());
        }

        List<RecordMeta> meta = handleRecordsListRead(() ->
            recordsResolver.getMeta(new ArrayList<>(records), attributes, rawAtts));

        return meta != null ? meta : Collections.emptyList();
    }

    /* META */

    @NotNull
    @Override
    public <T> T getMeta(@NotNull RecordRef recordRef, @NotNull Class<T> metaClass) {

        List<T> meta = getMeta(Collections.singletonList(recordRef), metaClass);
        if (meta.size() == 0) {
            throw new IllegalStateException("Can't get record metadata. Ref: " + recordRef + " Result: " + meta);
        }
        return meta.get(0);
    }

    @NotNull
    @Override
    public <T> List<T> getMeta(@NotNull Collection<RecordRef> records, @NotNull Class<T> metaClass) {

        Map<String, String> attributes = dtoAttributesResolver.getAttributes(metaClass);
        if (attributes.isEmpty()) {
            log.warn("Attributes is empty. Query will return empty meta. MetaClass: " + metaClass);
        }

        List<RecordMeta> meta = getAtts(records, attributes);

        return meta.stream()
            .map(m -> recordsMetaService.instantiateMeta(metaClass, m))
            .collect(Collectors.toList());
    }

    /* MODIFICATION */

    @NotNull
    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        Map<String, RecordRef> aliasToRecordRef = new HashMap<>();
        RecordsMutResult result = new RecordsMutResult();

        List<RecordMeta> records = mutation.getRecords();

        for (int i = records.size() - 1; i >= 0; i--) {

            RecordMeta record = records.get(i);

            ObjectData attributes = ObjectData.create();

            record.forEach((name, value) -> {

                String simpleName = name;

                if (name.charAt(0) != '.') {

                    int dotIdx = name.indexOf('.', 1);
                    int bracketIdx = name.indexOf('{');

                    int nameEndIdx = dotIdx;
                    if (dotIdx == -1 || bracketIdx != -1 && bracketIdx < dotIdx) {
                        nameEndIdx = bracketIdx;
                    }

                    if (nameEndIdx > 0) {
                        simpleName = name.substring(0, nameEndIdx);
                    } else {
                        int questionIdx = name.indexOf('?');
                        if (questionIdx > 0) {
                            simpleName = name.substring(0, questionIdx);
                        }
                    }

                } else {

                    Matcher matcher = ATT_PATTERN.matcher(name);
                    if (matcher.matches()) {
                        simpleName = matcher.group(2);
                    } else {
                        simpleName = null;
                    }
                }

                if (StringUtils.isNotBlank(simpleName)) {

                    if (name.endsWith("?assoc") || name.endsWith("{assoc}") || name.endsWith("{.assoc}")) {
                        value = convertAssocValue(value, aliasToRecordRef);
                    }

                    attributes.set(simpleName, value);
                }
            });

            record.setAttributes(attributes);

            RecordsMutation sourceMut = new RecordsMutation();
            sourceMut.setRecords(Collections.singletonList(record));
            RecordsMutResult recordMutResult = recordsResolver.mutate(sourceMut);
            if (recordMutResult == null) {
                recordMutResult = new RecordsMutResult();
                recordMutResult.setRecords(sourceMut.getRecords()
                    .stream()
                    .map(r -> new RecordMeta(r.getId()))
                    .collect(Collectors.toList()));
            }

            if (i == 0) {
                result.merge(recordMutResult);
            }

            List<RecordMeta> resultRecords = recordMutResult.getRecords();
            for (RecordMeta resultMeta : resultRecords) {
                String alias = record.get(RecordConstants.ATT_ALIAS, "");
                if (StringUtils.isNotBlank(alias)) {
                    aliasToRecordRef.put(alias, resultMeta.getId());
                }
            }
        }

        return result;
    }

    private DataValue convertAssocValue(DataValue value, Map<String, RecordRef> mapping) {
        if (value.isTextual()) {
            String textValue = value.asText();
            if (mapping.containsKey(textValue)) {
                return DataValue.create(mapping.get(textValue).toString());
            }
        } else if (value.isArray()) {
            List<DataValue> convertedValue = new ArrayList<>();
            for (DataValue node : value) {
                convertedValue.add(convertAssocValue(node, mapping));
            }
            return DataValue.create(convertedValue);
        }
        return value;
    }

    @NotNull
    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        RecordsDelResult result = recordsResolver.delete(deletion);
        if (result == null) {
            result = new RecordsDelResult();
            result.setRecords(deletion.getRecords().stream().map(RecordMeta::new).collect(Collectors.toList()));
        }
        return result;
    }

    /* OTHER */

    private <T> RecsQueryRes<T> handleRecordsQuery(Supplier<RecsQueryRes<T>> supplier) {
        return handleRecordsRead(supplier, RecsQueryRes::new);
    }

    private <T extends RecordsResult<?>> T handleRecordsRead(Supplier<T> impl, Supplier<T> orElse) {

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

    private <T> List<T> handleRecordsListRead(Supplier<List<T>> impl) {

        List<T> result;

        try {
            result = QueryContext.withContext(serviceFactory, impl);
        } catch (Throwable e) {
            log.error("Records resolving error", e);
            result = Collections.emptyList();
        }

        return result;
    }

    @Nullable
    @Override
    public RecsSourceInfo getSourceInfo(String sourceId) {
        return recordsResolver.getSourceInfo(sourceId);
    }

    @NotNull
    @Override
    public List<RecsSourceInfo> getSourcesInfo() {
        return recordsResolver.getSourceInfo();
    }

    @Override
    public void register(RecordsDao recordsSource) {

        String id = recordsSource.getId();
        if (id == null) {
            throw new IllegalArgumentException("id is a mandatory parameter for RecordsDao");
        }
        register(id, recordsSource);
    }

    @Override
    public void register(String sourceId, RecordsDao recordsSource) {
        if (recordsResolver instanceof RecordsDaoRegistry) {
            ((RecordsDaoRegistry) recordsResolver).register(sourceId, recordsSource);
        } else {
            log.warn("Records resolver doesn't support source registration. "
                + "Source: " + sourceId + " " + recordsSource.getClass());
        }
    }
}
