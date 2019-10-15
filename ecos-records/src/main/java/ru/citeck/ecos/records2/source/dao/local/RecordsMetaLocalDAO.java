package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.source.dao.RecordsMetaDAO;

import java.util.List;

public interface RecordsMetaLocalDAO<T> extends RecordsMetaDAO {

    List<T> getMetaValues(List<RecordRef> records);
}
