package ru.citeck.ecos.records2.source.common;

import kotlin.Deprecated;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdge;
import ru.citeck.ecos.records2.graphql.meta.value.MetaEdgeDelegate;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.List;
import java.util.function.Supplier;

/**
 * Custom attributes for any RecordsDao.
 *
 * @param <ReqMetaT> Map&lt;String, String&gt; or DTO Class
 * @param <ResMetaT> Map&lt;String, String&gt; or DTO Class or RecordMeta
 */
@Deprecated(message = "Replace with AttMixin. You can use new mixins with old Records DAO!")
public interface AttributesMixin<ReqMetaT, ResMetaT> {

    List<String> getAttributesList();

    Object getAttribute(String attribute, ResMetaT meta, MetaField field) throws Exception;

    default MetaEdge getEdge(String attribute, ResMetaT meta, Supplier<MetaEdge> base, MetaField field) {
        return new MetaEdgeDelegate(base.get(), f -> getAttribute(attribute, meta, f));
    }

    /**
     * Metadata to request from record.
     *
     * @return Map&lt;String, String&gt; or DTO instance
     */
    ReqMetaT getMetaToRequest();
}
