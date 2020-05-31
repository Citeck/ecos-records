package ru.citeck.ecos.records2.source.dao.local;

import lombok.extern.slf4j.Slf4j;

/**
 * Deprecated.
 * @deprecated use LocalRecordsDao instead
 */
@Slf4j
public abstract class LocalRecordsDAO extends LocalRecordsDao {

    public LocalRecordsDAO(boolean addSourceId) {
        super(addSourceId);
    }

    public LocalRecordsDAO() {
    }
}
