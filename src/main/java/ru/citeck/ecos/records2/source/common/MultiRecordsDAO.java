package ru.citeck.ecos.records2.source.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.AbstractRecordsDAO;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDAO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pavel Simonov
 */
public class MultiRecordsDAO extends AbstractRecordsDAO
                             implements RecordsQueryDAO {

    private static final Log logger = LogFactory.getLog(MultiRecordsDAO.class);

    private List<RecordsQueryDAO> recordsDao;
    private Map<String, RecordsQueryDAO> daoBySource = new ConcurrentHashMap<>();

    @Override
    public RecordsQueryResult<RecordRef> queryRecords(RecordsQuery query) {

        RecordsQueryResult<RecordRef> result = new RecordsQueryResult<>();

        RecordsQuery localQuery = new RecordsQuery(query);

        int sourceIdx = 0;
        RecordRef afterId = localQuery.getAfterId();
        if (afterId != RecordRef.EMPTY) {
            String source = afterId.getSourceId();
            while (sourceIdx < recordsDao.size() && !recordsDao.get(sourceIdx).getId().equals(source)) {
                sourceIdx++;
            }
        }

        while (sourceIdx < recordsDao.size() && result.getRecords().size() < query.getMaxItems()) {

            localQuery.setMaxItems(query.getMaxItems() - result.getRecords().size());
            RecordsQueryDAO recordsDAO = recordsDao.get(sourceIdx);
            RecordsQueryResult<RecordRef> daoRecords = recordsDAO.queryRecords(localQuery);

            result.merge(daoRecords);

            if (++sourceIdx < recordsDao.size()) {

                result.setHasMore(true);

                if (localQuery.isAfterIdMode()) {
                    localQuery.setAfterId(null);
                } else {
                    long skip = localQuery.getSkipCount() - daoRecords.getTotalCount();
                    localQuery.setSkipCount((int) Math.max(skip, 0));
                }
            }
        }

        if (result.getTotalCount() == query.getMaxItems() && result.getHasMore()) {
            result.setTotalCount(result.getTotalCount() + 1);
        }

        return result;
    }

    public void setRecordsDao(List<RecordsQueryDAO> records) {
        this.recordsDao = records;
        records.forEach(r -> daoBySource.put(r.getId(), r));
    }
}
