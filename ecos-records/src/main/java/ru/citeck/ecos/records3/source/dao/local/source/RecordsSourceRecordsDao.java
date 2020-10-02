package ru.citeck.ecos.records3.source.dao.local.source;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.operation.meta.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.EmptyValue;
import ru.citeck.ecos.records3.record.operation.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;
import ru.citeck.ecos.records3.source.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.source.info.RecsSourceInfo;

import java.util.List;
import java.util.stream.Collectors;

public class RecordsSourceRecordsDao extends AbstractRecordsDao
                                     implements RecordsQueryDao,
                                                RecordsAttsDao {

    public static final String ID = "source";

    public RecordsSourceRecordsDao() {
        setId(ID);
    }

    @Override
    public List<?> getRecordsAtts(List<String> records) {

        return records.stream().map(rec -> {
            RecsSourceInfo info = recordsService.getSourceInfo(rec);
            if (info == null) {
                return EmptyValue.INSTANCE;
            }
            return info;
        }).collect(Collectors.toList());
    }

    @Override
    public RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        return new RecordsQueryRes<>(recordsService.getSourcesInfo());
    }
}
