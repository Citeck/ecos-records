package ru.citeck.ecos.records2.graphql.meta.value;

import graphql.language.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetaFieldImpl implements MetaField {

    private final Field field;

    public MetaFieldImpl(Field field) {
        this.field = field;
    }

    @Override
    public String getInnerSchema() {
        StringBuilder sb = new StringBuilder();
        fillInnerSchema(field, sb);
        return sb.toString();
    }

    private void fillInnerSchema(Field field, StringBuilder sb) {

        for (Field innerField : getInnerFields(field)) {

            sb.append(innerField.getName());

            List<Argument> args = innerField.getArguments();

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

            List<Field> innerInnerFields = getInnerFields(innerField);

            if (innerInnerFields.size() > 0) {

                sb.append("{");

                for (Field innerInnerfield : innerInnerFields) {
                    fillInnerSchema(innerInnerfield, sb);
                }

                sb.append("}");
            }
        }
    }

    @Override
    public List<String> getInnerAttributes() {

        List<String> attributes = new ArrayList<>();

        for (Field field : getInnerFields(field)) {

            if (field.getName().startsWith("att")) {

                field.getArguments()
                     .stream()
                     .findFirst()
                     .filter(a -> a.getValue() instanceof StringValue)
                     .map(a -> ((StringValue) a.getValue()).getValue())
                     .ifPresent(attributes::add);
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
