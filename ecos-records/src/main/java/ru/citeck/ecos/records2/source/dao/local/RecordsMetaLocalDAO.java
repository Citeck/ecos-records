package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;

import java.util.List;

/**
 * Interface to receive metadata from records.
 * @deprecated use interface from v2 package instead:
 *             ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO
 */
@Deprecated
public interface RecordsMetaLocalDAO<T> extends LocalRecordsMetaDAO<T> {

    List<T> getMetaValues(List<RecordRef> records);

    @Override
    default List<T> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return getMetaValues(records);
    }
}
