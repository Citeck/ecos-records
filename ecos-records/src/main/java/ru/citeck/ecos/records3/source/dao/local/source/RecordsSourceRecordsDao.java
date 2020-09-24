package ru.citeck.ecos.records3.source.dao.local.source;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records3.record.op.query.request.query.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.request.query.RecsQueryRes;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsQueryDao;
import ru.citeck.ecos.records3.source.info.RecordsSourceInfo;

import java.util.List;
import java.util.stream.Collectors;

public class RecordsSourceRecordsDao extends LocalRecordsDao
                                     implements LocalRecordsQueryDao,
                                                LocalRecordsMetaDao {

    public static final String ID = "source";

    public RecordsSourceRecordsDao() {
        setId(ID);
    }

    @Override
    public List<?> getLocalRecordsMeta(List<RecordRef> records) {

        return records.stream().map(rec -> {
            RecordsSourceInfo info = recordsService.getSourceInfo(rec.getId());
            if (info == null) {
                return EmptyValue.INSTANCE;
            }
            return info;
        }).collect(Collectors.toList());
    }

    @Override
    public RecsQueryRes<?> queryLocalRecords(@NotNull RecordsQuery query) {
        return new RecsQueryRes<>(recordsService.getSourcesInfo());
    }
}
