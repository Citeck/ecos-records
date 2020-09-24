package ru.citeck.ecos.records3.source.dao;

public interface RecordsDao {

    String getId();

    /**
     * Is this DAO provides raw attributes (not flat).
     */
    default boolean isRawAttributesProvided() {
        return true;
    }
}
