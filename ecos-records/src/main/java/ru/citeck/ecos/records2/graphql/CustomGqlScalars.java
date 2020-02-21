package ru.citeck.ecos.records2.graphql;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.*;
import graphql.language.*;
import graphql.schema.*;
import ru.citeck.ecos.records2.utils.json.JsonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CustomGqlScalars {

    public static final GraphQLScalarType JSON_NODE =  new GraphQLScalarType(
            "JsonNode",
            "Json node scalar",
            new JsonNodeCoercing()
    );

    private static class JsonNodeCoercing implements Coercing<JsonNode, JsonNode> {

        private JsonNode convertImpl(Object value) {
            if (value instanceof String) {
                try {
                    return JsonUtils.readTree((String) value);
                } catch (Exception e) {
                    throw new CoercingParseValueException("Json string is incorrect: " + value, e);
                }
            } else if (value instanceof JsonNode) {
                return (JsonNode) value;
            }
            try {
                return JsonUtils.valueToTree(value);
            } catch (Exception e) {
                throw new CoercingParseValueException(e);
            }
        }

        @Override
        public JsonNode serialize(Object value) throws CoercingSerializeException {
            return convertImpl(value);
        }

        @Override
        public JsonNode parseValue(Object input) throws CoercingParseValueException {
            return convertImpl(input);
        }

        @Override
        public JsonNode parseLiteral(Object input) throws CoercingParseLiteralException {
            return parseLiteral(input, Collections.emptyMap());
        }

        @Override
        public JsonNode parseLiteral(Object input, Map<String, Object> variables) throws CoercingParseLiteralException {
            if (!(input instanceof Value)) {
                throw new CoercingParseLiteralException("Expected AST type 'Value' but was '" + typeName(input) + "'.");
            }
            if (input instanceof NullValue) {
                return null;
            }
            if (input instanceof FloatValue) {
                return FloatNode.valueOf(((FloatValue) input).getValue().floatValue());
            }
            if (input instanceof StringValue) {
                return TextNode.valueOf(((StringValue) input).getValue());
            }
            if (input instanceof IntValue) {
                return IntNode.valueOf(((IntValue) input).getValue().intValue());
            }
            if (input instanceof BooleanValue) {
                return BooleanNode.valueOf(((BooleanValue) input).isValue());
            }
            if (input instanceof EnumValue) {
                return TextNode.valueOf(((EnumValue) input).getName());
            }
            if (input instanceof VariableReference) {

                String varName = ((VariableReference) input).getName();
                Object value = variables.get(varName);

                if (value instanceof String) {
                    return TextNode.valueOf((String) value);
                } else if (value instanceof Integer) {
                    return IntNode.valueOf((Integer) value);
                } else if (value instanceof Long) {
                    return LongNode.valueOf((Long) value);
                } else if (value instanceof Float) {
                    return FloatNode.valueOf((Float) value);
                } else if (value instanceof Double) {
                    return DoubleNode.valueOf((Double) value);
                }

                throw new CoercingParseLiteralException(
                    "Variable value type is not supported: '" + typeName(value) + "'.");
            }
            if (input instanceof ArrayValue) {
                List<Value> values = ((ArrayValue) input).getValues();
                ArrayNode result = JsonUtils.createArrayNode();
                values.forEach(v -> result.add(parseLiteral(v, variables)));
                return result;
            }
            if (input instanceof ObjectValue) {
                List<ObjectField> values = ((ObjectValue) input).getObjectFields();
                ObjectNode result = JsonUtils.createObjectNode();
                values.forEach(fld -> {
                    JsonNode parsedValue = parseLiteral(fld.getValue(), variables);
                    result.put(fld.getName(), parsedValue);
                });
                return result;
            }

            throw new CoercingParseLiteralException("Type is not supported: '" + typeName(input) + "'.");
        }
    }

    private static String typeName(Object input) {
        if (input == null) {
            return "null";
        }
        return input.getClass().getSimpleName();
    }
}
