package ru.citeck.ecos.records3.record.resolver;

import ru.citeck.ecos.records3.source.dao.RecordsDao;

public interface RecordsDaoRegistry {

    void register(String sourceId, RecordsDao recordsDao);
}
