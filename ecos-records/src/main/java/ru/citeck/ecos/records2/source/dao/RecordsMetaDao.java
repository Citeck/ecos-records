package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;

import java.util.List;

@Deprecated
public interface RecordsMetaDao extends RecordsDao {

    RecordsResult<RecordAtts> getMeta(List<RecordRef> records, List<SchemaAtt> schema, boolean rawAtts);
}
