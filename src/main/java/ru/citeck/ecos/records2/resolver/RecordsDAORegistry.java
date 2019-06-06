package ru.citeck.ecos.records2.resolver;

import ru.citeck.ecos.records2.source.dao.RecordsDAO;

public interface RecordsDAORegistry {

    void register(RecordsDAO recordsDao);
}
