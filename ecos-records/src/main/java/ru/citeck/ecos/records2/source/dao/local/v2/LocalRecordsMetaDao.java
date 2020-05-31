package ru.citeck.ecos.records2.source.dao.local.v2;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDao;

import java.util.List;

public interface LocalRecordsMetaDao<T> extends RecordsMetaDao {

    List<T> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField);
}
