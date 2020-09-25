package ru.citeck.ecos.records3.source.dao.local;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.*;
import ru.citeck.ecos.records3.graphql.meta.value.MetaValuesConverter;
import ru.citeck.ecos.records3.record.operation.meta.RecordAttsService;
import ru.citeck.ecos.records3.predicate.PredicateService;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutResult;
import ru.citeck.ecos.records3.record.operation.mutate.request.RecordsMutation;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.request.result.RecordsResult;
import ru.citeck.ecos.records3.source.common.AttributesMixin;
import ru.citeck.ecos.records3.source.common.ParameterizedAttsMixin;
import ru.citeck.ecos.records3.source.dao.*;
import ru.citeck.ecos.records3.source.dao.local.v2.*;
import ru.citeck.ecos.records3.type.RecordTypeService;

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
 *
 * @author Pavel Simonov
 */
@Slf4j
@SuppressWarnings("unchecked")
public abstract class LocalRecordsDao extends AbstractRecordsDao implements ServiceFactoryAware {

    protected RecordsService recordsService;
    protected PredicateService predicateService;
    protected RecordAttsService recordsMetaService;
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
    public RecsQueryRes<Object> queryRecords(@NotNull RecordsQuery query) {

        /*RecordsQueryResult<RecordMeta> queryResult = new RecordsQueryResult<>();

        List<RecordRef> recordRefs = new ArrayList<>();
        List<Object> rawMetaValues = new ArrayList<>();

        if (this instanceof LocalRecordsQueryDao) {

            LocalRecordsQueryDao withMeta = (LocalRecordsQueryDao) this;

            RecordsQueryResult<?> values = withMeta.queryLocalRecords(query, schema.getMetaField());

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
        }

        if (!recordRefs.isEmpty()) {

            if (this instanceof LocalRecordsMetaDao) {

                LocalRecordsMetaDao metaDao = (LocalRecordsMetaDao) this;
                rawMetaValues.addAll(metaDao.getLocalRecordsMeta(recordRefs, schema.getMetaField()));
            } else if (this instanceof RecordsMetaDao) {
                RecordsMetaDao metaDao = (RecordsMetaDao) this;
                RecordsResult<RecordMeta> meta = metaDao.getMeta(recordRefs, schema);
                queryResult.merge(meta);
            } else {
                writeWarn("RecordsDao implements neither LocalRecordsMetaDao "
                          + "nor RecordsMetaDao. We can't receive metadata");
                recordRefs.stream().map(RecordMeta::new).forEach(queryResult::addRecord);
            }
        }

        if (!rawMetaValues.isEmpty()) {
            queryResult.merge(getMetaImpl(rawMetaValues, schema));
        }

        if (addSourceId) {
            queryResult.setRecords(RecordsUtils.convertToRefs(getId(), queryResult.getRecords()));
        }

        return queryResult;*/
        return null;
    }

    @NotNull
    public List<Object> getRecordsMeta(@NotNull List<RecordRef> records) {

        /*RecordsResult<RecordMeta> result;

        if (this instanceof LocalRecordsMetaDao) {

            LocalRecordsMetaDao metaLocalDao = (LocalRecordsMetaDao) this;

            List<RecordRef> localRecords = addSourceId ? RecordsUtils.toLocalRecords(records) : records;
            List<?> metaValues = metaLocalDao.getLocalRecordsMeta(localRecords, schema.getMetaField());

            result = getMetaImpl(metaValues, schema);

        } else {

            writeWarn("RecordsDao doesn't implement LocalRecordsMetaDao. We can't receive metadata");

            result = new RecordsResult<>();
            records.stream().map(RecordMeta::new).forEach(result::addRecord);
        }

        if (addSourceId) {
            result.setRecords(RecordsUtils.convertToRefs(getId(), result.getRecords()));
        }

        return result;*/

        return null;
    }

    private RecordsResult<RecordMeta> getMetaImpl(List<?> records) {

        /*Map<Object, Object> metaCache = new ConcurrentHashMap<>();

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

        return recordsMetaService.getMeta(recordsWithMixin, schema.getGqlSchema());*/

        return null;
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
