package ru.citeck.ecos.records3.source.dao.local.v2;

import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.MutableRecordsLocalDao;

public abstract class LocalRecordsCrudDao<T> extends LocalRecordsDao
                                             implements LocalRecordsMetaDao,
                                                        MutableRecordsLocalDao<T> {
}
