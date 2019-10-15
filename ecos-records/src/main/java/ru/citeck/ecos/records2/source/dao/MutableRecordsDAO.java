package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;

public interface MutableRecordsDAO extends RecordsDAO {

    RecordsMutResult mutate(RecordsMutation mutation);

    RecordsDelResult delete(RecordsDeletion deletion);
}
