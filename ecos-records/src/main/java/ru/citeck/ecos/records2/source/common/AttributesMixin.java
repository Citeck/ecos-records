package ru.citeck.ecos.records2.source.common;

import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdgeDelegate;

import java.util.List;

/**
 * Custom attributes for any RecordsDao.
 *
 * @param <ReqMetaT> Map&lt;String, String&gt; or DTO Class
 * @param <ResMetaT> Map&lt;String, String&gt; or DTO Class or RecordMeta
 */
public interface AttributesMixin<ReqMetaT, ResMetaT> {

    List<String> getAttributesList();

    Object getAttribute(String attribute, ResMetaT meta) throws Exception;

    default MetaEdge getEdge(String attribute, ResMetaT meta, UncheckedSupplier<MetaEdge> base) throws Exception {
        return new MetaEdgeDelegate(base.get(), () -> getAttribute(attribute, meta));
    }

    /**
     * Metadata to request from record.
     *
     * @return Map&lt;String, String&gt; or DTO instance
     */
    ReqMetaT getMetaToRequest();
}
