package ru.citeck.ecos.records3.source.common;

import ru.citeck.ecos.commons.utils.func.UncheckedSupplier;
import ru.citeck.ecos.records3.record.operation.meta.value.AttEdge;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.MetaEdgeDelegate;

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

    default AttEdge getEdge(String attribute, ResMetaT meta, UncheckedSupplier<AttEdge> base) throws Exception {
        return new MetaEdgeDelegate(base.get(), () -> getAttribute(attribute, meta));
    }

    /**
     * Metadata to request from record.
     *
     * @return Map&lt;String, String&gt; or DTO instance
     */
    ReqMetaT getMetaToRequest();
}
