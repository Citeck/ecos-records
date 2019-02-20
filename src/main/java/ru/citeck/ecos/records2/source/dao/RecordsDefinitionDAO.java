package ru.citeck.ecos.records2.source.dao;

import ru.citeck.ecos.records2.source.MetaAttributeDef;

import java.util.Collection;
import java.util.List;

public interface RecordsDefinitionDAO extends RecordsDAO {

    List<MetaAttributeDef> getAttributesDef(Collection<String> names);
}
