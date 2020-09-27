package ru.citeck.ecos.records3.source.common;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.source.dao.RecordsQueryDao;

import java.util.List;
import java.util.function.Function;

public abstract class FilteredRecordsDao extends AbstractRecordsDao implements RecordsQueryDao {

    private RecordsQueryDao targetDao;

    @Override
    public RecordsQueryRes<RecordAtts> queryRecords(@NotNull RecordsQuery query) {

        /* todo
        RecordsQuery localQuery = new RecordsQuery(query);
        int maxItems = localQuery.getMaxItems();
        localQuery.setMaxItems((int) (1.5f * maxItems));

        RecordsQueryResult<RecordMeta> records = targetDao.queryRecords(localQuery);
        Function<List<RecordMeta>, List<RecordMeta>> filter = getFilter(query);

        List<RecordMeta> filtered = filter.apply(records.getRecords());
        List<RecordMeta> resultRecords = new ArrayList<>();

        int itemsCount = Math.min(filtered.size(), maxItems);
        for (int i = 0; i < itemsCount; i++) {
            resultRecords.add(filtered.get(i));
        }

        int totalDiff = records.getRecords().size() - filtered.size();
        records.setTotalCount(records.getTotalCount() - totalDiff);
        records.setRecords(resultRecords);


        return records;
 */
        return null;
    }

    protected abstract Function<List<RecordAtts>, List<RecordAtts>> getFilter(RecordsQuery query);

    public void setTargetDao(RecordsQueryDao targetDao) {
        this.targetDao = targetDao;
    }
}
