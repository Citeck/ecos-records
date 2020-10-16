package ru.citeck.ecos.records3.record.op.query.dao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.record.dao.RecordsDao;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;

import java.util.Collections;
import java.util.List;

public interface RecordsQueryDao extends RecordsDao {

    @Nullable
    RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query);

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
