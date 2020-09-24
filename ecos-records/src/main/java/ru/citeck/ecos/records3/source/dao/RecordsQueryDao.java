package ru.citeck.ecos.records3.source.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.operation.query.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.RecsQueryRes;

import java.util.Collections;
import java.util.List;

/**
 * Base interface for Dao which can search records.
 *
 * @author Pavel Simonov
 */
public interface RecordsQueryDao extends RecordsDao {

    @Nullable
    RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query);

    /**
     * Get query languages which can be used to query records in this DAO.
     * First languages in the result list are more preferred than last
     *
     * @return list of languages
     */
    @Nullable
    default List<String> getSupportedLanguages() {
        return Collections.emptyList();
    }

    default boolean isGroupingSupported() {
        return false;
    }
}
