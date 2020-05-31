package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.records2.source.dao.RecordsDao;

public interface RecordsDaoRegistry {

    void register(RecordsDao recordsDao);
}
