package ru.citeck.ecos.records2.graphql.meta.value;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class MetaEdgeValue implements MetaValue {

    private final MetaEdge edge;

    @Override
    public Object getAttribute(@NotNull String name, @NotNull MetaField field) throws Exception {
        switch (name) {
            case "name":
                return edge.getName();
            case "val":
                return edge.getValue(field);
            case "vals":
                Object value = edge.getValue(field);
                if (value == null) {
                    return Collections.emptyList();
                }
                if (value instanceof Collection) {
                    return new ArrayList<Object>((Collection<?>) value);
                }
                return Collections.singletonList(value);
            case "title":
                return edge.getTitle();
            case "description":
                return edge.getDescription();
            case "protected":
                return edge.isProtected();
            case "canBeRead":
                return edge.canBeRead();
            case "multiple":
                return edge.isMultiple();
            case "options":
                return edge.getOptions();
            case "javaClass":
                return edge.getJavaClass();
            case "editorKey":
                return edge.getEditorKey();
            case "type":
                return edge.getType();
            case "distinct":
                return edge.getDistinct();
            case "createVariants":
                return edge.getCreateVariants();
            case "isAssoc":
                return edge.isAssociation();
        }
        return null;
    }
}
