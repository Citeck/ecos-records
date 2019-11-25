package ru.citeck.ecos.records2.source.dao.local.v2;

import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDAO;

public abstract class LocalRecordsCrudDAO<T> extends LocalRecordsDAO
                                             implements LocalRecordsMetaDAO<T>,
                                                        LocalRecordsQueryWithMetaDAO<T>,
                                                        MutableRecordsLocalDAO<T> {
}
