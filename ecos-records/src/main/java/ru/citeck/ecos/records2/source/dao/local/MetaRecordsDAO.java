package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordsServiceFactory;

/**
 * Deprecated.
 * @deprecated use MetaRecordsDao instead
 */
@Deprecated
public class MetaRecordsDAO extends MetaRecordsDao {

    public MetaRecordsDAO(RecordsServiceFactory serviceFactory) {
        super(serviceFactory);
    }
}
