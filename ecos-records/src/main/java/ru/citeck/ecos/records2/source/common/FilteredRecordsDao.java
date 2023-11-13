package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class FilteredRecordsDao extends AbstractRecordsDao implements RecordsQueryDao {

    private RecordsQueryDao targetDao;

    @Override
    public RecordsQueryResult<EntityRef> queryRecords(RecordsQuery query) {

        RecordsQuery localQuery = new RecordsQuery(query);
        int maxItems = localQuery.getMaxItems();
        localQuery.setMaxItems((int) (1.5f * maxItems));

        RecordsQueryResult<EntityRef> records = targetDao.queryRecords(localQuery);
        Function<List<EntityRef>, List<EntityRef>> filter = getFilter(query);

        List<EntityRef> filtered = filter.apply(records.getRecords());
        List<EntityRef> resultRecords = new ArrayList<>();

        int itemsCount = Math.min(filtered.size(), maxItems);
        for (int i = 0; i < itemsCount; i++) {
            resultRecords.add(filtered.get(i));
        }

        int totalDiff = records.getRecords().size() - filtered.size();
        records.setTotalCount(records.getTotalCount() - totalDiff);
        records.setRecords(resultRecords);

        return records;
    }

    protected abstract Function<List<EntityRef>, List<EntityRef>> getFilter(RecordsQuery query);

    public void setTargetDao(RecordsQueryDao targetDao) {
        this.targetDao = targetDao;
    }
}
