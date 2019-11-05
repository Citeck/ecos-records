package ru.citeck.ecos.records2.graphql.meta.value;

import graphql.language.*;

import java.util.*;

public class MetaFieldImpl implements MetaField {

    private final Field field;

    public MetaFieldImpl(Field field) {
        this.field = field;
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

                Value value = arg.getValue();
                if (value instanceof StringValue) {
                    sb.append("\"").append(((StringValue) value).getValue()).append("\"");
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