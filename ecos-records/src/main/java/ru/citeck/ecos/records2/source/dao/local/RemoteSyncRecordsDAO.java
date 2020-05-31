package ru.citeck.ecos.records2.source.dao.local;

/**
 * Deprecated.
 * @deprecated use RemoteSyncRecordsDao instead
 */
@Deprecated
public class RemoteSyncRecordsDAO<T> extends RemoteSyncRecordsDao<T> {

    public RemoteSyncRecordsDAO(String sourceId, Class<T> model) {
        super(sourceId, model);
    }
}
