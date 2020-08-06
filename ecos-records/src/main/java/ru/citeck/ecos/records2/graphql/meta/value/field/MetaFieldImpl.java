package ru.citeck.ecos.records2.graphql.meta.value.field;

import graphql.language.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;

import java.util.*;

public class MetaFieldImpl implements MetaField {

    private final Field field;
    @Getter
    private final MetaField parent;

    public MetaFieldImpl(Field field, MetaField parent) {
        this.field = field;
        this.parent = parent;
    }

    public MetaFieldImpl(Field field) {
        this.field = field;
        this.parent = EmptyMetaField.INSTANCE;
    }

    @Override
    public String getInnerSchema() {
        return getFieldSchema(field);
    }

    @Override
    public String getAlias() {
        return field.getAlias();
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public String getAttributeSchema(String fieldName) {
        return getFieldSchema(getInnerAttFields().get(fieldName));
    }

    @NotNull
    @Override
    public Map<String, MetaField> getSubFields() {

        Map<String, MetaField> subFields = new HashMap<>();

        List<Field> innerFields = getInnerFields(field);
        innerFields.forEach(f -> {
            MetaField field = new MetaFieldImpl(f, this);
            subFields.put(field.getKey(), field);
        });

        return subFields;
    }

    private String getFieldSchema(Field field) {
        if (field == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        fillFieldsSchema(getInnerFields(field), sb);
        return sb.toString();
    }

    private void fillFieldsSchema(List<Field> fields, StringBuilder sb) {

        for (int i = 0; i < fields.size(); i++) {

            Field innerField = fields.get(i);

            if (innerField.getAlias() != null) {
                sb.append(innerField.getAlias()).append(":");
            }

            fillFieldSchema(innerField, sb);

            if (i < fields.size() - 1) {
                sb.append(",");
            }
        }
    }

    private void fillFieldSchema(Field field, StringBuilder sb) {

        sb.append(field.getName());

        List<Argument> args = field.getArguments();

        if (args.size() > 0) {

            sb.append("(");

            for (Argument arg : args) {

                sb.append(arg.getName()).append(":");

                Value<?> value = arg.getValue();
                if (value instanceof StringValue) {
                    String strArg = ((StringValue) value).getValue();
                    strArg = strArg.replace("\"", "\\\"");
                    sb.append("\"").append(strArg).append("\"");
                } else {
                    throw new IllegalArgumentException("Unknown type: " + value);
                }
            }

            sb.append(")");
        }

        List<Field> innerInnerFields = getInnerFields(field);

        if (innerInnerFields.size() > 0) {
            sb.append("{");
            fillFieldsSchema(innerInnerFields, sb);
            sb.append("}");
        }
    }

    @Override
    public List<String> getInnerAttributes() {
        return new ArrayList<>(getInnerAttFields().keySet());
    }

    @Override
    public Map<String, String> getInnerAttributesMap() {

        Map<String, String> result = new HashMap<>();
        Map<String, Field> fields = getInnerAttFields();

        StringBuilder sb = new StringBuilder();
        fields.forEach((k, field) -> {
            sb.setLength(0);
            fillFieldSchema(field, sb);
            result.put(k, "." + sb.toString());
        });

        return result;
    }

    private Map<String, Field> getInnerAttFields() {

        Map<String, Field> attributes = new HashMap<>();

        for (Field field : getInnerFields(field)) {

            if (field.getName().startsWith("att")) {

                field.getArguments()
                     .stream()
                     .findFirst()
                     .filter(a -> a.getValue() instanceof StringValue)
                     .map(a -> ((StringValue) a.getValue()).getValue())
                     .ifPresent(name -> attributes.put(name, field));
            } else {
                attributes.put("." + field.getName(), field);
            }
        }

        return attributes;
    }

    private List<Field> getInnerFields(Field field) {

        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet == null) {
            return Collections.emptyList();
        }

        List<Selection> selections = selectionSet.getSelections();
        if (selections == null || selections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Field> fields = new ArrayList<>();

        for (Selection selection : selections) {

            if (selection instanceof Field) {
                fields.add((Field) selection);
            }
        }

        return fields;
    }
}
