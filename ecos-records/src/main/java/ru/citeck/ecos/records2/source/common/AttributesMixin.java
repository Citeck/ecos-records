package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.SimpleMetaEdge;

import java.util.List;

/**
 * Custom attributes for any RecordsDao.
 *
 * @param <ReqMetaT> Map&lt;String, String&gt; or DTO Class
 * @param <ResMetaT> Map&lt;String, String&gt; or DTO Class or RecordMeta
 */
public interface AttributesMixin<ReqMetaT, ResMetaT> {

    List<String> getAttributesList();

    Object getAttribute(String attribute, ResMetaT meta, MetaField field) throws Exception;

    default MetaEdge getEdge(String attribute, ResMetaT meta, MetaField field) {
        return new SimpleMetaEdge(attribute, f -> getAttribute(attribute, meta, f));
    }

    /**
     * Metadata to request from record.
     *
     * @return Map&lt;String, String&gt; or DTO instance
     */
    ReqMetaT getMetaToRequest();
}
