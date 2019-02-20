package ru.citeck.ecos.records2.source;

public interface MetaAttributeDef {

    String getName();

    String getTitle();

    Class<?> getJavaClass();

    boolean isMultiple();
}
