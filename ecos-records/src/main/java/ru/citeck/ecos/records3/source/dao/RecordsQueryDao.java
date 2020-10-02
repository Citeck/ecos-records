package ru.citeck.ecos.records3.source.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.operation.query.dto.RecordsQueryRes;

import java.util.Collections;
import java.util.List;

/**
 * Base interface for Dao which can search records.
 *
 * @author Pavel Simonov
 */
public interface RecordsQueryDao extends RecordsDao {

    @Nullable
    RecordsQueryRes<?> queryRecords(@NotNull RecordsQuery query);

    /**
     * Get query languages which can be used to query records in this DAO.
     * First languages in the result list are more preferred than last
     *
     * @return list of languages
     */
    @NotNull
    default List<String> getSupportedLanguages() {
        return Collections.emptyList();
    }

    default boolean isGroupingSupported() {
        return false;
    }
}
