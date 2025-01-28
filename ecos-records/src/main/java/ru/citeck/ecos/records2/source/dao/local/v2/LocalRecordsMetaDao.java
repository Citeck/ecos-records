package ru.citeck.ecos.records2.source.dao.local.v2;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDao;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

/**
 * @deprecated should be replaced with Record(s)AttsDao
 */
@Deprecated
public interface LocalRecordsMetaDao<T> extends RecordsMetaDao {

    List<T> getLocalRecordsMeta(@NotNull List<EntityRef> records, @NotNull MetaField metaField);
}
