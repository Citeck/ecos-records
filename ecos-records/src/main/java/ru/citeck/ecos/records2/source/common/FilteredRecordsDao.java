package ru.citeck.ecos.records2.source.common;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.meta.schema.AttsSchema;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records2.source.dao.RecordsQueryDao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class FilteredRecordsDao extends AbstractRecordsDao implements RecordsQueryDao {

    private RecordsQueryDao targetDao;


    @Override
    public RecordsQueryResult<RecordMeta> queryRecords(@NotNull RecordsQuery query,
                                                       @NotNull AttsSchema schema) {

        RecordsQuery localQuery = new RecordsQuery(query);
        int maxItems = localQuery.getMaxItems();
        localQuery.setMaxItems((int) (1.5f * maxItems));

        RecordsQueryResult<RecordMeta> records = targetDao.queryRecords(localQuery, schema);
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
    }

    protected abstract Function<List<RecordMeta>, List<RecordMeta>> getFilter(RecordsQuery query);

    public void setTargetDao(RecordsQueryDao targetDao) {
        this.targetDao = targetDao;
    }
}
