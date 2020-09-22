package ru.citeck.ecos.records2.meta.schema.read;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.meta.attproc.AttProcessorDef;
import ru.citeck.ecos.records2.meta.schema.AttsSchema;
import ru.citeck.ecos.records2.meta.schema.GqlKeyUtils;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class AttSchemaReader {

    private static final String KEYS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String GQL_ONE_ARG_ATT = "^%s\\((n:)?['\"](.+?)['\"]\\)";
    private static final String GQL_ONE_ARG_ATT_WITH_INNER = GQL_ONE_ARG_ATT + "\\{(.+)}";

    private static final Pattern GQL_ATT_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "atts?"));
    private static final Pattern GQL_EDGE_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "edge"));
    private static final Pattern GQL_AS_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "as"));
    private static final Pattern GQL_HAS_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT, "has"));

    private static final Pattern PROCESSOR_PATTERN = Pattern.compile("(.+?)\\((.*)\\)");

    @NotNull
    public AttsSchema read(@Nullable Map<String, String> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            return new AttsSchema();
        }

        List<SchemaAtt> schemaAtts = new ArrayList<>();
        attributes.forEach((k, v) -> schemaAtts.add(readAttribute(k, v)));

        return new AttsSchema(attributes, schemaAtts);
    }

    @Nullable
    public SchemaAtt readAttribute(String attribute) {
        return readAttribute("", attribute);
    }

    @Nullable
    public SchemaAtt readAttribute(String alias, String attribute) {

        if (StringUtils.isBlank(attribute)) {
            throw new AttReadException(alias, attribute, "Empty attribute");
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

        if (att.charAt(0) == '#') {

            int questionIdx = att.indexOf('?');
            if (questionIdx == -1) {
                throw new AttReadException(
                    alias,
                    attribute,
                    "Scalar type is mandatory for attributes with #. E.g.: #name?protected"
                );
            }
            att = ".edge(n:\"" + attribute.substring(1, questionIdx) + "\"){"
                + attribute.substring(questionIdx + 1) + "}";
        }
        if (att.charAt(0) == '.') {
            return readDotAtt(alias, att.substring(1), processors);
        }
        return readSimpleAtt(alias, att, processors);
    }

    private List<SchemaAtt> readInnerAtts(String innerAtts,
                                          boolean dotContext,
                                          BiFunction<String, String, SchemaAtt> parserFunc) {

        List<String> innerAttsList = AttStrUtils.split(innerAtts, ",");
        boolean multipleAtts = innerAttsList.size() > 1;
        AtomicInteger idx = new AtomicInteger(0);

        return innerAttsList
            .stream()
            .map(att -> readInnerAtt(att, dotContext, multipleAtts, idx.getAndIncrement(), parserFunc))
            .collect(Collectors.toList());
    }

    private SchemaAtt readInnerAtt(String innerAtt,
                                   boolean dotContext,
                                   boolean multipleAtts,
                                   int idx,
                                   BiFunction<String, String, SchemaAtt> parserFunc) {

        String att = innerAtt.trim();
        int aliasDelimIdx = AttStrUtils.indexOf(att, ":");

        String alias = "";
        if (aliasDelimIdx > 0) {
            alias = GqlKeyUtils.unescape(att.substring(0, aliasDelimIdx));
            att = att.substring(aliasDelimIdx + 1);
        }

        if (alias.isEmpty() && multipleAtts) {
            if (dotContext) {
                alias = "" + KEYS.charAt(idx);
            } else {
                int questionIdx = att.indexOf('?');
                if (questionIdx >= 0) {
                    alias = att.substring(0, questionIdx);
                } else {
                    alias = att;
                }
            }
        }
        return parserFunc.apply(alias, dotContext ? "." + att : att);
    }

    private SchemaAtt readLastSimpleAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        String att = attribute;
        int questionIdx = AttStrUtils.indexOf(att, "?");
        if (questionIdx != -1) {
            att = att.substring(0, questionIdx) + "{." + att.substring(questionIdx + 1) + "}";
        }

        int arrIdx = AttStrUtils.indexOf(att, "[]");
        boolean isMultiple = false;
        if (arrIdx != -1) {
            isMultiple = true;
            att = att.substring(0, arrIdx) + att.substring(arrIdx + 2);
        }

        int openBraceIdx = AttStrUtils.indexOf(att, '{');
        if (openBraceIdx == -1) {
            openBraceIdx = att.length();
            att += "{.disp}";
        }
        int closeBraceIdx = AttStrUtils.indexOf(att, '}');
        if (closeBraceIdx == -1) {
            closeBraceIdx = att.length();
            att += "}";
        }

        return new SchemaAtt(
            alias,
            attribute.substring(0, openBraceIdx),
            isMultiple,
            readInnerAtts(att.substring(openBraceIdx + 1, closeBraceIdx), false, this::readAttribute),
            processors
        );
    }


    private SchemaAtt readSimpleAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        List<String> dotPath = AttStrUtils.split(attribute, '.');

        String lastPathElem = dotPath.get(dotPath.size() - 1);

        SchemaAtt schemaAtt = readLastSimpleAtt(
            dotPath.size() == 1 ? alias : "",
            lastPathElem,
            processors
        );

        for (int i = dotPath.size() - 2; i >= 0; i--) {

            String pathElem = dotPath.get(i);

            int arrIdx = AttStrUtils.indexOf(pathElem, "[]");
            boolean isMultiple = false;
            if (arrIdx != -1) {
                isMultiple = true;
                pathElem = pathElem.substring(0, arrIdx) + pathElem.substring(arrIdx + 2);
            }

            schemaAtt = new SchemaAtt(
                i == 0 ? alias : "",
                pathElem,
                isMultiple,
                Collections.singletonList(schemaAtt)
            );
        }

        return schemaAtt;
    }

    /**
     * @param attribute - attribute without dot
     */
    private SchemaAtt readDotAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        if (attribute.startsWith("att")) {
            return parseGqlAtt(alias, attribute, processors);
        }
        if (attribute.startsWith("edge")) {
            return readEdgeAtt(alias, attribute, processors);
        }

        int braceIdx = AttStrUtils.indexOf(attribute, '(');
        if (braceIdx == -1) {
            return new SchemaAtt(alias, "." + attribute);
        }

        if (attribute.startsWith("as")) {
            return parseAsAtt(alias, attribute, processors);
        }

        if (attribute.startsWith("has")) {
            return parseHasAtt(alias, attribute, processors);
        }

        throw new AttReadException(alias, attribute, "Unknown dot attribute");
    }

    private SchemaAtt parseAsAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        Matcher matcher = GQL_AS_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            throw new AttReadException(alias, attribute, "Incorrect 'as' attribute");
        }

        String attName  = matcher.group(2);
        String attInner = matcher.group(3);

        return new SchemaAtt(
            alias,
            RecordConstants.ATT_AS,
            false,
            Collections.singletonList(new SchemaAtt(
                "",
                attName,
                false,
                readInnerAtts(attInner, true, this::readAttribute),
                processors
            ))
        );
    }

    private SchemaAtt parseHasAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        Matcher matcher = GQL_HAS_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            throw new AttReadException(alias, attribute, "Incorrect 'has' attribute");
        }

        String attName = matcher.group(2);

        return new SchemaAtt(
            alias,
            RecordConstants.ATT_HAS,
            false,
            Collections.singletonList(new SchemaAtt(
                "",
                attName,
                false,
                Collections.singletonList(new SchemaAtt(".bool")),
                processors
            ))
        );
    }

    private SchemaAtt readEdgeAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        Matcher matcher = GQL_EDGE_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            throw new AttReadException(alias, attribute, "Incorrect 'edge' attribute");
        }

        String attName  = matcher.group(2);
        String attInner = matcher.group(3);

        List<String> innerAtts = AttStrUtils.split(attInner, ",");
        List<SchemaAtt> schemaInnerAtts = new ArrayList<>();
        boolean multipleAtts = innerAtts.size() > 1;

        int idx = 0;
        for (String innerAttWithAlias : innerAtts) {
            schemaInnerAtts.add(readInnerAtt(
                innerAttWithAlias,
                false,
                multipleAtts,
                idx++, this::parseEdgeInnerAtt
            ));
        }

        return new SchemaAtt(
            alias,
            RecordConstants.ATT_EDGE,
            false,
            Collections.singletonList(new SchemaAtt("", attName, false, schemaInnerAtts)),
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

        String attWithoutInner = attribute.substring(0, openBraceIdx);
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
                    readInnerAtts(innerAtts, true, this::readAttribute),
                    Collections.emptyList()
                );
        }

        throw new AttReadException(alias, attribute, "Unknown 'edge inner' attribute");
    }

    private SchemaAtt parseGqlAtt(String alias, String attribute, List<AttProcessorDef> processors) {

        Matcher matcher = GQL_ATT_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            log.error("Incorrect att attribute: '" + attribute);
            return null;
        }

        String attName  = matcher.group(2);
        boolean multiple = attribute.startsWith("atts");
        List<SchemaAtt> innerAtts = readInnerAtts(matcher.group(3), true, this::readAttribute);

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
