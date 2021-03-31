package ru.citeck.ecos.records2.graphql.meta.value;

import lombok.Getter;
import ru.citeck.ecos.commons.utils.func.UncheckedFunction;

import java.util.List;

/**
 * @deprecated -> AttEdgeDelegate
 */
@Deprecated
public class MetaEdgeDelegate implements MetaEdge {

    @Getter
    private final MetaEdge impl;
    private final UncheckedFunction<MetaField, Object> getValue;

    public MetaEdgeDelegate(MetaEdge impl) {
        this.impl = impl;
        this.getValue = null;
    }

    public MetaEdgeDelegate(MetaEdge impl, UncheckedFunction<MetaField, Object> getValue) {
        this.impl = impl;
        this.getValue = getValue;
    }

    public boolean isProtected() {
        return impl.isProtected();
    }

    public boolean isMultiple() {
        return impl.isMultiple();
    }

    public boolean isAssociation() {
        return impl.isAssociation();
    }

    public boolean isSearchable() {
        return impl.isSearchable();
    }

    public List<?> getOptions() {
        return impl.getOptions();
    }

    public List<?> getDistinct() {
        return impl.getDistinct();
    }

    public List<CreateVariant> getCreateVariants() {
        return impl.getCreateVariants();
    }

    public Class<?> getJavaClass() {
        return impl.getJavaClass();
    }

    public String getEditorKey() {
        return impl.getEditorKey();
    }

    public String getType() {
        return impl.getType();
    }

    public String getTitle() {
        return impl.getTitle();
    }

    public String getDescription() {
        return impl.getDescription();
    }

    public String getName() {
        return impl.getName();
    }

    public Object getValue(MetaField field) throws Exception {
        if (getValue != null) {
            return getValue.apply(field);
        }
        return impl.getValue(field);
    }
}
