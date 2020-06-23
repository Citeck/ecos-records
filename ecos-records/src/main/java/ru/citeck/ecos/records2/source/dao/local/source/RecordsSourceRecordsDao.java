package ru.citeck.ecos.records2.source.dao.local.source;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.records2.source.info.RecordsSourceInfo;

import java.util.List;
import java.util.stream.Collectors;

public class RecordsSourceRecordsDao extends LocalRecordsDao
                                     implements LocalRecordsMetaDao<Object>,
                                                LocalRecordsQueryWithMetaDao<RecordsSourceInfo> {

    public static final String ID = "source";

    public RecordsSourceRecordsDao() {
        setId(ID);
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {

        return records.stream().map(rec -> {
            RecordsSourceInfo info = recordsService.getSourceInfo(rec.getId());
            if (info == null) {
                return EmptyValue.INSTANCE;
            }
            return info;
        }).collect(Collectors.toList());
    }

    @Override
    public RecordsQueryResult<RecordsSourceInfo> queryLocalRecords(RecordsQuery query, MetaField field) {
        return new RecordsQueryResult<>(recordsService.getSourcesInfo());
    }
}
