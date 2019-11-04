package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsCrudDAO;

/**
 * Interface to receive metadata from records.
 * @deprecated use class from v2 package instead:
 *             ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsCrudDAO
 */
@Deprecated
public abstract class CrudRecordsDAO<T> extends LocalRecordsCrudDAO<T>
                                        implements RecordsMetaLocalDAO<T>,
                                                   RecordsQueryWithMetaLocalDAO<T>,
                                                   MutableRecordsLocalDAO<T> {
}
