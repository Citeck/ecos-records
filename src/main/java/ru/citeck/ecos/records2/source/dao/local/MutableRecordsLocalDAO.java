package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.MutableRecordsDAO;

import java.util.List;

public interface MutableRecordsLocalDAO<T> extends MutableRecordsDAO {

    List<T> getValuesToMutate(List<RecordRef> records);

    RecordsMutResult save(List<T> values);
}
