package ru.citeck.ecos.records2.source.dao.local.v2;

import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;

@Deprecated
public abstract class LocalRecordsCrudDao<T> extends LocalRecordsDao
                                             implements LocalRecordsMetaDao<T>,
                                                        LocalRecordsQueryWithMetaDao<T>,
                                                        MutableRecordsLocalDao<T> {
}
