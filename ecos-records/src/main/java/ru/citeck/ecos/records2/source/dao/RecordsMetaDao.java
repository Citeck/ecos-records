package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

/**
 * @deprecated -> Record(s)AttsDao
 */
@Deprecated
public interface RecordsMetaDao extends RecordsDao {

    RecordsResult<RecordAtts> getMeta(List<EntityRef> records, List<SchemaAtt> schema, boolean rawAtts);
}
