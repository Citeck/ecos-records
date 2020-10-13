package ru.citeck.ecos.records2.source.dao.local;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.graphql.RecordsMetaGql;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.common.AttributesMixin;
import ru.citeck.ecos.records2.source.common.AttributesMixinMetaValue;
import ru.citeck.ecos.records2.source.common.ParameterizedAttsMixin;
import ru.citeck.ecos.records2.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.records2.type.RecordTypeService;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Local records DAO.
 *
 * <p>
 * Extend this DAO if your data located in the same application instance
 * (when you don't want to execute graphql query remotely)
 * Important: This class implement only RecordsDao.
 * All other interfaces should be implemented by inherited classes
 * </p>
 *
 * @see LocalRecordsQueryDao
 * @see LocalRecordsMetaDao
 * @see LocalRecordsQueryWithMetaDao
 *
 * @author Pavel Simonov
 */
@Slf4j
@SuppressWarnings("unchecked")
public abstract class LocalRecordsDao extends AbstractRecordsDao implements ServiceFactoryAware {

    protected RecordsService recordsService;
    protected PredicateService predicateService;
    protected RecordsMetaService recordsMetaService;
    protected RecordsMetaGql recordsMetaGql;
    protected MetaValuesConverter metaValuesConverter;
    protected RecordsServiceFactory serviceFactory;
    protected RecordTypeService recordTypeService;

    private boolean addSourceId = true;
    private final Map<String, ParameterizedAttsMixin> mixins = new ConcurrentHashMap<>();

    public LocalRecordsDao() {
    }

    public LocalRecordsDao(boolean addSourceId) {
        this.addSourceId = addSourceId;
    }

    @NotNull
    public final RecordsMutResult mutate(@NotNull RecordsMutation mutation) {
        return mutateImpl(mutation);
    }

    @NotNull
    protected RecordsMutResult mutateImpl(@NotNull RecordsMutation mutation) {

        if (this instanceof MutableRecordsLocalDao) {

            MutableRecordsLocalDao mutableDao = (MutableRecordsLocalDao) this;

            List<RecordRef> recordRefs = mutation.getRecords()
                    .stream()
                    .map(meta -> addSourceId ? RecordRef.valueOf(meta.getId().getId()) : meta.getId())
                    .collect(Collectors.toList());

            List<?> values = mutableDao.getValuesToMutate(recordRefs);

            if (values.size() != recordRefs.size()) {
                throw new IllegalStateException(this + " getValuesToMutate returned a wrong number of values\n"
                                                + "recordRefs: " + recordRefs + "\n"
                                                + "values    : " + values);
            }

            for (int i = 0; i < recordRefs.size(); i++) {
                RecordMeta meta = mutation.getRecords().get(i);
                Json.getMapper().applyData(values.get(i), meta.getAttributes());
            }

            RecordsMutResult result = mutableDao.save(values);
            if (addSourceId) {
                result.setRecords(result.getRecords()
                        .stream()
                        .map(meta -> new RecordMeta(meta, r -> RecordRef.valueOf(getId() + "@" + r)))
                        .collect(Collectors.toList()));
            }

            return result;
        }

        writeWarn("RecordsDao doesn't implement MutableRecordsLocalDAO");

        return new RecordsMutResult();
    }

    @NotNull
    public RecordsQueryResult<RecordRef> queryRecords(@NotNull RecordsQuery query) {

        if (this instanceof LocalRecordsQueryDao) {

            LocalRecordsQueryDao recordsQueryLocalDao = (LocalRecordsQueryDao) this;

            RecordsQueryResult<RecordRef> localRecords = recordsQueryLocalDao.queryLocalRecords(query);
            if (addSourceId) {
                return new RecordsQueryResult<>(localRecords, r -> RecordRef.valueOf(getId() + "@" + r));
            }
            return localRecords;

        } else if (this instanceof LocalRecordsQueryWithMetaDao) {

            RecordsQueryResult<RecordMeta> records = queryRecords(query, "id");
            return new RecordsQueryResult<>(records, RecordMeta::getId);
        }

        writeWarn("RecordsDao doesn't implement neither "
                  + "LocalRecordsQueryDao nor LocalRecordsQueryWithMetaDao");

        return new RecordsQueryResult<>();
    }

    @NotNull
    public RecordsQueryResult<RecordMeta> queryRecords(@NotNull RecordsQuery query, @NotNull String metaSchema) {

        RecordsQueryResult<RecordMeta> queryResult = new RecordsQueryResult<>();

        List<RecordRef> recordRefs = new ArrayList<>();
        List<Object> rawMetaValues = new ArrayList<>();

        MetaField metaField = null;

        if (this instanceof LocalRecordsQueryWithMetaDao) {

            LocalRecordsQueryWithMetaDao withMeta = (LocalRecordsQueryWithMetaDao) this;
            metaField = recordsMetaGql.getMetaFieldFromSchema(metaSchema);

            RecordsQueryResult<?> values = withMeta.queryLocalRecords(query, metaField);

            queryResult.merge(values);
            queryResult.setHasMore(values.getHasMore());
            queryResult.setTotalCount(values.getTotalCount());

            for (Object record : values.getRecords()) {
                if (record instanceof RecordRef) {
                    recordRefs.add((RecordRef) record);
                } else {
                    rawMetaValues.add(record);
                }
            }

        } else if (this instanceof RecordsQueryDao) {

            RecordsQueryDao recordsQueryDao = (RecordsQueryDao) this;
            RecordsQueryResult<RecordRef> records = recordsQueryDao.queryRecords(query);
            queryResult.merge(records);
            queryResult.setHasMore(records.getHasMore());
            queryResult.setTotalCount(records.getTotalCount());

            recordRefs.addAll(records.getRecords());
        }

        if (!recordRefs.isEmpty()) {

            if (this instanceof LocalRecordsMetaDao) {

                if (metaField == null) {
                    metaField = recordsMetaGql.getMetaFieldFromSchema(metaSchema);
                }

                LocalRecordsMetaDao metaDao = (LocalRecordsMetaDao) this;
                rawMetaValues.addAll(metaDao.getLocalRecordsMeta(recordRefs, metaField));
            } else if (this instanceof RecordsMetaDao) {
                RecordsMetaDao metaDao = (RecordsMetaDao) this;
                RecordsResult<RecordMeta> meta = metaDao.getMeta(recordRefs, metaSchema);
                queryResult.merge(meta);
            } else {
                writeWarn("RecordsDao implements neither LocalRecordsMetaDao "
                          + "nor RecordsMetaDao. We can't receive metadata");
                recordRefs.stream().map(RecordMeta::new).forEach(queryResult::addRecord);
            }
        }

        if (!rawMetaValues.isEmpty()) {
            queryResult.merge(getMetaImpl(rawMetaValues, metaSchema));
        }

        if (addSourceId) {
            queryResult.setRecords(RecordsUtils.convertToRefsMeta(getId(), queryResult.getRecords()));
        }

        return queryResult;
    }

    @NotNull
    public RecordsResult<RecordMeta> getMeta(@NotNull List<RecordRef> records, @NotNull String metaSchema) {

        RecordsResult<RecordMeta> result;

        if (this instanceof LocalRecordsMetaDao) {

            LocalRecordsMetaDao metaLocalDao = (LocalRecordsMetaDao) this;

            MetaField metaField = recordsMetaGql.getMetaFieldFromSchema(metaSchema);

            List<RecordRef> localRecords = addSourceId ? RecordsUtils.toLocalRecords(records) : records;
            List<?> metaValues = metaLocalDao.getLocalRecordsMeta(localRecords, metaField);

            result = getMetaImpl(metaValues, metaSchema);

        } else {

            writeWarn("RecordsDao doesn't implement LocalRecordsMetaDao. We can't receive metadata");

            result = new RecordsResult<>();
            records.stream().map(RecordMeta::new).forEach(result::addRecord);
        }

        if (addSourceId) {
            result.setRecords(RecordsUtils.convertToRefsMeta(getId(), result.getRecords()));
        }

        return result;
    }

    private RecordsResult<RecordMeta> getMetaImpl(List<?> records, String schema) {

        Map<Object, Object> metaCache = new ConcurrentHashMap<>();

        List<?> recordsWithMixin = records.stream()
            .map(r -> metaValuesConverter.toMetaValue(r))
            .map(mv ->
                new AttributesMixinMetaValue(mv,
                    recordsMetaService,
                    recordTypeService,
                    metaValuesConverter,
                    mixins,
                    metaCache
                ))
            .collect(Collectors.toList());

        return recordsMetaService.getMeta(recordsWithMixin, schema);
    }

    protected void writeWarn(String msg) {
        log.warn(toString() + ": " + msg);
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        recordsService = serviceFactory.getRecordsService();
        predicateService = serviceFactory.getPredicateService();
        recordsMetaService = serviceFactory.getRecordsMetaService();
        recordsMetaGql = serviceFactory.getRecordsMetaGql();
        metaValuesConverter = serviceFactory.getMetaValuesConverter();
        recordTypeService = serviceFactory.getRecordTypeService();
    }

    public void addAttributesMixin(AttributesMixin<?, ?> mixin) {

        ParameterizedAttsMixin paramMixin = new ParameterizedAttsMixin(mixin);

        Set<String> atts = new HashSet<>(mixin.getAttributesList());
        atts.forEach(a -> {
            if (mixins.containsKey(a)) {
                log.error("Mixin tries to replace existing attribute. "
                        + "It's not allowed. Attribute: " + a + " mixin: " + mixin);
            } else {
                mixins.put(a, paramMixin);
            }
        });
        new ParameterizedAttsMixin(mixin);
    }

    /**
     * Remove attributes mixin by reference equality.
     */
    public void removeAttributesMixin(AttributesMixin<?, ?> mixin) {

        AttributesMixin<Object, Object> typedMixin = (AttributesMixin<Object, Object>) mixin;

        Set<String> attsToRemove = new HashSet<>();
        mixin.getAttributesList().forEach(a -> {
            ParameterizedAttsMixin parameterizedAttsMixin = mixins.get(a);
            if (parameterizedAttsMixin != null && parameterizedAttsMixin.getImpl() == typedMixin) {
                attsToRemove.add(a);
            }
        });
        attsToRemove.forEach(mixins::remove);
    }

    @Override
    public String toString() {
        return "[" + getId() + "](" + getClass().getName() + "@" + Integer.toHexString(hashCode()) + ")";
    }
}
