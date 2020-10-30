package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records3.record.op.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;

import java.util.List;

@Deprecated
public interface RecordsQueryWithMetaDao extends RecordsQueryBaseDao {

    RecordsQueryResult<RecordAtts> queryRecords(RecordsQuery query, List<SchemaAtt> schema, boolean rawAtts);
}
