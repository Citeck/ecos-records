package ru.citeck.ecos.records2.source.dao.local;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.records2.*;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.meta.RecordsMetaServiceAware;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.*;
import ru.citeck.ecos.records2.utils.RecordsUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Local records DAO.
 *
 * <p>
 * Extend this DAO if your data located in the same alfresco instance
 * (when you don't want to execute graphql query remotely)
 * Important: This class implement only RecordsDAO.
 * All other interfaces should be implemented in children classes
 * </p>
 *
 * @see RecordsQueryDAO
 * @see RecordsMetaDAO
 * @see RecordsQueryWithMetaDAO
 * @see MutableRecordsDAO
 *
 * @author Pavel Simonov
 */
@SuppressWarnings("unchecked")
public abstract class LocalRecordsDAO extends AbstractRecordsDAO implements RecordsMetaServiceAware,
                                                                            RecordsServiceAware,
                                                                            PredicateServiceAware {

    private static final Log logger = LogFactory.getLog(LocalRecordsDAO.class);

    protected RecordsService recordsService;
    protected PredicateService predicateService;
    protected RecordsMetaService recordsMetaService;

    protected ObjectMapper objectMapper = new ObjectMapper();

    private boolean addSourceId = true;

    {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public LocalRecordsDAO() {
    }

    public LocalRecordsDAO(boolean addSourceId) {
        this.addSourceId = addSourceId;
    }

    public RecordsMutResult mutate(RecordsMutation mutation) {

        List<RecordRef> recordRefs = mutation.getRecords()
                                             .stream()
                                             .map(RecordMeta::getId)
                                             .collect(Collectors.toList());

        if (this instanceof MutableRecordsLocalDAO) {

            MutableRecordsLocalDAO mutableDao = (MutableRecordsLocalDAO) this;
            List<?> values = mutableDao.getValuesToMutate(recordRefs);

            for (int i = 0; i < recordRefs.size(); i++) {

                RecordMeta meta = mutation.getRecords().get(i);

                try {
                    objectMapper.readerForUpdating(values.get(i)).readValue(meta.getAttributes());
                } catch (IOException e) {
                    throw new RuntimeException("Mutation failed", e);
                }
            }

            return mutableDao.save(values);
        }

        logger.warn("[" + getId() + "] RecordsDAO doesn't implement MutableRecordsLocalDAO");

        return new RecordsMutResult();
    }

    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {

        if (this instanceof RecordsQueryLocalDAO) {

            RecordsQueryLocalDAO recordsQueryLocalDAO = (RecordsQueryLocalDAO) this;

            RecordsQueryResult<RecordRef> localRecords = recordsQueryLocalDAO.getLocalRecords(query);
            if (addSourceId) {
                return new RecordsQueryResult<>(localRecords, r -> RecordRef.create(getId(), r));
            }
            return localRecords;

        } else {

            RecordsQueryResult<RecordMeta> records = queryRecords(query, "id");
            return new RecordsQueryResult<>(records, RecordMeta::getId);
        }
    }

    public RecordsQueryResult<RecordMeta> queryRecords(RecordsQuery query, String metaSchema) {

        RecordsQueryResult<RecordMeta> queryResult = new RecordsQueryResult<>();

        List<RecordRef> recordRefs = new ArrayList<>();
        List<Object> rawMetaValues = new ArrayList<>();

        if (this instanceof RecordsQueryWithMetaLocalDAO) {

            RecordsQueryWithMetaLocalDAO withMeta = (RecordsQueryWithMetaLocalDAO) this;
            RecordsQueryResult<?> values = withMeta.getMetaValues(query);

            if (values != null) {

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
            } else {
                logger.warn("[" + getId() + "] getMetaValues(query) return null");
            }

        } else if (this instanceof RecordsQueryDAO) {

            RecordsQueryDAO recordsQueryDAO = (RecordsQueryDAO) this;
            RecordsQueryResult<RecordRef> records = recordsQueryDAO.queryRecords(query);
            queryResult.merge(records);
            queryResult.setHasMore(records.getHasMore());
            queryResult.setTotalCount(records.getTotalCount());

            recordRefs.addAll(records.getRecords());
        }

        if (!recordRefs.isEmpty()) {
            if (this instanceof RecordsMetaLocalDAO) {
                RecordsMetaLocalDAO metaDao = (RecordsMetaLocalDAO) this;
                rawMetaValues.addAll(metaDao.getMetaValues(recordRefs));
            } else if (this instanceof RecordsMetaDAO) {
                RecordsMetaDAO metaDao = (RecordsMetaDAO) this;
                RecordsResult<RecordMeta> meta = metaDao.getMeta(recordRefs, metaSchema);
                queryResult.merge(meta);
            } else {
                logger.warn("[" + getId() + "] RecordsDAO implements neither "
                            + "RecordsMetaLocalDAO nor RecordsMetaDAO. We can't receive metadata");
                recordRefs.stream().map(RecordMeta::new).forEach(queryResult::addRecord);
            }
        }

        if (!rawMetaValues.isEmpty()) {
            queryResult.merge(recordsMetaService.getMeta(rawMetaValues, metaSchema));
        }

        if (addSourceId) {
            queryResult.setRecords(RecordsUtils.convertToRefs(getId(), queryResult.getRecords()));
        }

        return queryResult;
    }

    public RecordsResult<RecordMeta> getMeta(List<RecordRef> records, String metaSchema) {

        RecordsResult<RecordMeta> result;

        if (this instanceof RecordsMetaLocalDAO) {

            RecordsMetaLocalDAO metaLocalDao = (RecordsMetaLocalDAO) this;

            List<RecordRef> localRecords = addSourceId ? RecordsUtils.toLocalRecords(records) : records;
            List<?> metaValues = metaLocalDao.getMetaValues(localRecords);
            result = recordsMetaService.getMeta(metaValues, metaSchema);

        } else {

            logger.warn("[" + getId() + "] RecordsDAO doesn't implement "
                        + "RecordsMetaLocalDAO. We can't receive metadata");

            result = new RecordsResult<>();
            records.stream().map(RecordMeta::new).forEach(result::addRecord);
        }

        if (addSourceId) {
            result.setRecords(RecordsUtils.convertToRefs(getId(), result.getRecords()));
        }

        return result;
    }

    @Override
    public void setRecordsMetaService(RecordsMetaService recordsMetaService) {
        this.recordsMetaService = recordsMetaService;
    }

    @Override
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @Override
    public void setPredicateService(PredicateService predicateService) {
        this.predicateService = predicateService;
    }
}
