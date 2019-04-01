package ru.citeck.ecos.records2.source.dao;

import java.util.Collections;
import java.util.List;

/**
 * Base interface for DAO which can search records.
 *
 * @author Pavel Simonov
 *
 * @see RecordsQueryDAO
 * @see RecordsQueryWithMetaDAO
 */
public interface RecordsQueryBaseDAO extends RecordsDAO {

    /**
     * Get query languages which can be used to query records in this DAO.
     * First languages in the result list are more preferred than last
     *
     * @return list of languages
     */
    default List<String> getSupportedLanguages() {
        return Collections.emptyList();
    }
}
