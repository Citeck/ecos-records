package ru.citeck.ecos.records3.source.dao.local.meta;

public interface MetaRecordsDaoAttsProvider {

    Object getAttributes();

    void register(MetaAttributesSupplier supplier);
}
