package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.MutableRecordsDao;

import java.util.List;

public interface MutableRecordsLocalDao<T> extends MutableRecordsDao {

    List<T> getValuesToMutate(List<RecordRef> records);

    RecordsMutResult save(List<T> values);
}
