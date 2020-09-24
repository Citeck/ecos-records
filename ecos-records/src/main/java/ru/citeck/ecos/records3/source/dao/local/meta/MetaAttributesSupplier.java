package ru.citeck.ecos.records3.source.dao.local.meta;

import java.util.List;

public interface MetaAttributesSupplier {

    List<String> getAttributesList();

    Object getAttribute(String attribute) throws Exception;
}
