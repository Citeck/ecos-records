package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.List;

/**
 * Custom attributes for any RecordsDao.
 *
 * @param <ReqMetaT> Map&lt;String, String&gt; or DTO Class
 * @param <ResMetaT> Map&lt;String, String&gt; or DTO Class or RecordMeta
 */
public interface AttributesMixin<ReqMetaT, ResMetaT> {

    List<String> getAttributesList();

    Object getAttribute(String attribute, ResMetaT meta, MetaField field);

    /**
     * Metadata to request from record.
     *
     * @return Map&lt;String, String&gt; or DTO instance
     */
    ReqMetaT getMetaToRequest();
}
