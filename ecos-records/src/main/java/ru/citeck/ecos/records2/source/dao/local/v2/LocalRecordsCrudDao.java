package ru.citeck.ecos.records2.source.dao.local.v2;

import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;

/**
 * @deprecated you should implement RecordsQueryDao, Record(s)MutateDao, Record(s)DeleteDao, Record(s)AttsDao to create CRUD DAO
 */
@Deprecated
public abstract class LocalRecordsCrudDao<T> extends LocalRecordsDao
                                             implements LocalRecordsMetaDao<T>,
                                                        LocalRecordsQueryWithMetaDao<T>,
                                                        MutableRecordsLocalDao<T> {
}
