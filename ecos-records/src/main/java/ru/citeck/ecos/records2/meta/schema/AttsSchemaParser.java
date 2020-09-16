package ru.citeck.ecos.records2.meta.schema;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class AttsSchemaParser {

    private static final Pattern GQL_ATT_PATTERN = Pattern.compile("^atts?\\((n:)?['\"](.+?)['\"]\\)\\{(.+)}");
    private static final Pattern GQL_EDGE_PATTERN = Pattern.compile("^edge\\((n:)?['\"](.+?)['\"]\\)\\{(.+)}");
    private static final Pattern PROCESSOR_PATTERN = Pattern.compile("(.+?)\\((.*)\\)");

    @NotNull
    public AttsSchema parse(@Nullable Map<String, String> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            return new AttsSchema();
        }

        List<SchemaAtt> schemaAtts = new ArrayList<>();
        attributes.forEach((k, v) -> schemaAtts.add(parseAttribute(k, v)));

        return new AttsSchema(attributes, schemaAtts);
    }

    @Nullable
    public SchemaAtt parseAttribute(String attribute) {
        return parseAttribute(getAttDefaultAlias(attribute), attribute);
    }

    @Nullable
    public SchemaAtt parseAttribute(String alias, String attribute) {

        if (StringUtils.isBlank(attribute)) {
            return new SchemaAtt(".disp");
        }

        List<AttProcessorDef> processors;

        String att = attribute;

        int orElseDelimIdx = AttStrUtils.indexOf(att, '!');
        while (orElseDelimIdx > 0) {

            int nextDelim0 = AttStrUtils.indexOf(att, "|", orElseDelimIdx + 1);
            int nextDelim1 = AttStrUtils.indexOf(att, "!", orElseDelimIdx + 1);

            int nextDelim = nextDelim0 == -1 ? nextDelim1 :
                (nextDelim1 == -1 ? nextDelim0 : Math.min(nextDelim0, nextDelim1));
            if (nextDelim == -1) {
                nextDelim = att.length();
            }

            String orElsePart = att.substring(orElseDelimIdx + 1, nextDelim);
            att = att.substring(0, orElseDelimIdx) + "|or('" + orElsePart + "')"
                + (att.length() > nextDelim ? att.substring(nextDelim) : "");

            orElseDelimIdx = AttStrUtils.indexOf(att, '!');
        }

        int pipeDelimIdx = AttStrUtils.indexOf(att, '|');
        if (pipeDelimIdx > 0) {
            processors = parseProcessors(att.substring(pipeDelimIdx + 1));
            att = att.substring(0, pipeDelimIdx);
        } else {
            processors = Collections.emptyList();
        }

        if (att.charAt(0) == '.') {
            return parseDotAtt(alias, att.substring(1), processors);
        }
        return parseSimpleAtt(alias, att, processors);
    }

    private String getAttDefaultAlias(String attribute) {
        int braceIdx = attribute.indexOf('(');
        return braceIdx > 0 ? attribute.substring(0, braceIdx) : attribute;
    }

    private List<SchemaAtt> parseInnerAtts(String innerAtts,
                                           boolean dotContext,
                                           BiFunction<String, String, SchemaAtt> parserFunc) {

        return AttStrUtils.split(innerAtts, ",")
            .stream()
            .map(att -> parseInnerAtt(att, dotContext, parserFunc))
            .collect(Collectors.toList());
    }

    private SchemaAtt parseInnerAtt(String innerAtt,
                                    boolean dotContext,
                                    BiFunction<String, String, SchemaAtt> parserFunc) {

        innerAtt = innerAtt.trim();

        int aliasDelimIdx = AttStrUtils.indexOf(innerAtt, ":");
        String att = innerAtt;
        String alias;
        if (aliasDelimIdx > 0) {
            alias = att.substring(0, aliasDelimIdx);
            att = att.substring(aliasDelimIdx + 1);
        } else {
            alias = getAttDefaultAlias(att);
        }

        return parserFunc.apply(alias, dotContext ? "." + att : att);
    }

    private SchemaAtt parseSimpleAtt(String alias, String attribute, List<AttProcessorDef> processors) {
        return new SchemaAtt("");
    }

    /**
     * @param attribute - attribute without dot
     */
    private SchemaAtt parseDotAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        if (attribute.startsWith("att")) {
            return parseGqlAtt(alias, attribute, processors);
        }
        if (attribute.startsWith("edge")) {
            return parseEdgeAtt(alias, attribute, processors);
        }

        int braceIdx = attribute.indexOf('(');
        if (braceIdx == -1) {
            return new SchemaAtt(attribute);
        }

        if (attribute.startsWith("as")) {

        }

        return new SchemaAtt("");
    }

    private SchemaAtt parseEdgeAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        Matcher matcher = GQL_EDGE_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            log.error("Incorrect edge attribute: '" + attribute);
            return null;
        }

        String attName  = matcher.group(2);
        String attInner = matcher.group(3);

        List<String> innerAtts = AttStrUtils.split(attInner, ",");
        List<SchemaAtt> schemaInnerAtts = new ArrayList<>();

        for (String innerAttWithAlias : innerAtts) {
            schemaInnerAtts.add(parseInnerAtt(innerAttWithAlias, false, this::parseEdgeInnerAtt));
        }

        return new SchemaAtt(
            alias,
            RecordConstants.ATT_EDGE,
            false,
            Collections.singletonList(new SchemaAtt(attName, attName, false, schemaInnerAtts)),
            processors
        );
    }

    private SchemaAtt parseEdgeInnerAtt(String alias, String attribute) {

        switch (attribute) {
            case "name":
            case "title":
            case "description":
            case "javaClass":
            case "editorKey":
            case "type":
                return new SchemaAtt(
                    alias,
                    attribute,
                    false,
                    Collections.singletonList(new SchemaAtt(".str")),
                    Collections.emptyList()
                );
            case "protected":
            case "canBeRead":
            case "multiple":
            case "isAssoc":
                return new SchemaAtt(
                    alias,
                    attribute,
                    false,
                    Collections.singletonList(new SchemaAtt(".bool")),
                    Collections.emptyList()
                );
        }

        int openBraceIdx = attribute.indexOf('{');
        int closeBraceIdx = attribute.lastIndexOf('}');
        if (openBraceIdx == -1 || closeBraceIdx == -1 || openBraceIdx > closeBraceIdx) {
            log.error("Incorrect edge inner attribute: '" + attribute + "'");
            return new SchemaAtt("");
        }

        String attWithoutInner = attribute.substring(openBraceIdx);
        String innerAtts = attribute.substring(openBraceIdx + 1, closeBraceIdx);
        boolean multiple = !"val".equals(attWithoutInner);

        switch (attWithoutInner) {
            case "val":
            case "vals":
            case "options":
            case "distinct":
            case "createVariants":
                return new SchemaAtt(
                    alias,
                    attWithoutInner,
                    multiple,
                    parseInnerAtts(innerAtts, true, this::parseAttribute),
                    Collections.emptyList()
                );
        }

        return new SchemaAtt("");
    }

    private SchemaAtt parseGqlAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        Matcher matcher = GQL_ATT_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            log.error("Incorrect att attribute: '" + attribute);
            return null;
        }

        String attName  = matcher.group(2);
        boolean multiple = attribute.startsWith(".atts");
        List<SchemaAtt> innerAtts = parseInnerAtts(matcher.group(3), true, this::parseAttribute);

        return new SchemaAtt(alias, attName, multiple, innerAtts, processors);
    }

    private List<AttProcessorDef> parseProcessors(String processorStr) {

        if (StringUtils.isBlank(processorStr)) {
            return Collections.emptyList();
        }
        List<AttProcessorDef> result = new ArrayList<>();

        for (String proc : AttStrUtils.split(processorStr, '|')) {
            Matcher matcher = PROCESSOR_PATTERN.matcher(proc);
            if (matcher.matches()) {
                String type = matcher.group(1).trim();
                List<DataValue> args = parseArgs(matcher.group(2).trim());
                result.add(new AttProcessorDef(type, args));
            }
        }
        return result;
    }

    private List<DataValue> parseArgs(String argsStr) {

        List<DataValue> result = new ArrayList<>();

        if (StringUtils.isBlank(argsStr)) {
            return result;
        }

        argsStr = argsStr.replace("'", "\"");
        AttStrUtils.split(argsStr, ",").stream()
            .map(String::trim)
            .map(DataValue::create)
            .forEach(result::add);

        return result;
    }
}
