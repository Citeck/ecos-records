package ru.citeck.ecos.records3.source.common;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordMeta;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.source.dao.RecordsQueryDao;

import java.util.List;

/**
 * RecordsDao to union multiple sources.
 *
 * @author Pavel Simonov
 */
public class MultiRecordsDao extends AbstractRecordsDao
                             implements RecordsQueryDao {

    private List<RecordsQueryDao> recordsDao;

    @NotNull
    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {

        RecsQueryRes<RecordMeta> result = new RecsQueryRes<>();

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
            RecordsQueryDao recordsDao = this.recordsDao.get(sourceIdx);
            RecsQueryRes<?> daoRecords = recordsDao.queryRecords(localQuery);

            //todo
            result.merge(daoRecords);

            if (++sourceIdx < this.recordsDao.size()) {

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

    public void setRecordsDao(List<RecordsQueryDao> records) {
        this.recordsDao = records;
    }
}
