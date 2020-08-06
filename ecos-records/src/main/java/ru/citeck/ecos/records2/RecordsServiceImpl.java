package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.resolver.RecordsDaoRegistry;
import ru.citeck.ecos.records2.resolver.RecordsResolver;
import ru.citeck.ecos.records2.source.dao.RecordsDao;
import ru.citeck.ecos.records2.source.info.RecordsSourceInfo;

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

    public RecordsServiceImpl(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
        recordsResolver = serviceFactory.getRecordsResolver();
        recordsMetaService = serviceFactory.getRecordsMetaService();
    }

    /* QUERY */

    @NotNull
    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {
        return handleRecordsQuery(() -> {
            RecordsQueryResult<RecordMeta> metaResult = recordsResolver.queryRecords(query,
                Collections.emptyMap(), true);
            return new RecordsQueryResult<>(metaResult, RecordMeta::getId);
        });
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

        RecordsQueryResult<RecordMeta> result = handleRecordsQuery(() ->
            recordsResolver.queryRecords(query, attributes, true));

        if (result == null) {
            result = new RecordsQueryResult<>();
        }
        return result;
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

        RecordsResult<RecordMeta> meta = handleRecordsRead(() ->
            recordsResolver.getMeta(records, attributes, flatAttributes), RecordsResult::new);

        if (meta == null) {
            meta = new RecordsResult<>();
        }
        return meta;
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

    private <T> RecordsQueryResult<T> handleRecordsQuery(Supplier<RecordsQueryResult<T>> supplier) {
        return handleRecordsRead(supplier, RecordsQueryResult::new);
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

    @Nullable
    @Override
    public RecordsSourceInfo getSourceInfo(String sourceId) {
        return recordsResolver.getSourceInfo(sourceId);
    }

    @NotNull
    @Override
    public List<RecordsSourceInfo> getSourcesInfo() {
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
