package ru.citeck.ecos.records2.source.dao.local;

public abstract class CrudRecordsDAO<T> extends LocalRecordsDAO
                                        implements RecordsMetaLocalDAO<T>,
                                                   RecordsQueryWithMetaLocalDAO<T>,
                                                   MutableRecordsLocalDAO<T> {
}
