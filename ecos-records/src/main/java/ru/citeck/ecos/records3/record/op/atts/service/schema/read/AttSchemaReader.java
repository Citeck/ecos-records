package ru.citeck.ecos.records3.record.op.atts.service.schema.read;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.utils.NameUtils;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttOrElseProcessor;
import ru.citeck.ecos.records3.record.op.atts.service.proc.AttProcessorDef;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.SchemaRootAtt;
import ru.citeck.ecos.records3.record.op.atts.service.schema.exception.AttSchemaException;
import ru.citeck.ecos.records2.meta.util.AttStrUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class AttSchemaReader {

    private static final String GQL_ONE_ARG_ATT = "^%s\\((n:)?['\"](.+?)['\"]\\)";
    private static final String GQL_ONE_ARG_ATT_WITH_INNER = GQL_ONE_ARG_ATT + "\\s*\\{(.+)}";

    private static final Pattern GQL_ATT_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "atts?"));
    private static final Pattern GQL_ATT_WITHOUT_INNER_PATTERN =
        Pattern.compile(String.format(GQL_ONE_ARG_ATT, "atts?"));

    private static final Pattern GQL_EDGE_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "edge"));
    private static final Pattern GQL_AS_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT_WITH_INNER, "as"));
    private static final Pattern GQL_HAS_PATTERN = Pattern.compile(String.format(GQL_ONE_ARG_ATT, "has"));
    private static final Pattern GQL_SIMPLE_AS_PATTERN = Pattern.compile("\\?as\\((n:)?[\\'\"](.+?)[\\'\"]\\)");

    private static final Pattern PROCESSOR_PATTERN = Pattern.compile("(.+?)\\((.*)\\)");

    @NotNull
    public List<SchemaRootAtt> readRoot(@Nullable Collection<String> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> atts = new LinkedHashMap<>();
        attributes.forEach(att -> atts.put(att, att));
        return readRoot(atts);
    }

    @NotNull
    public List<SchemaRootAtt> readRoot(@Nullable Map<String, String> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<SchemaRootAtt> schemaAtts = new ArrayList<>();
        attributes.forEach((k, v) -> schemaAtts.add(readRoot(k, v)));

        return schemaAtts;
    }

    /**
     * @throws AttReadException when attribute can't be read
     */
    public SchemaRootAtt readRoot(String attribute) {
        return readRoot("", attribute);
    }

    public SchemaAtt readInner(String alias, String attribute) {
        return readInner(alias, attribute, Collections.emptyList());
    }

    public SchemaAtt readInner(String alias, String attribute, List<SchemaAtt> innerAtts) {

        if (attribute.charAt(0) == '#') {

            int questionIdx = attribute.indexOf('?');
            if (questionIdx == -1) {
                throw new AttReadException(
                    alias,
                    attribute,
                    "Scalar type is mandatory for attributes with #. E.g.: #name?protected"
                );
            } else {
                if (attribute.endsWith("?options")) {
                    attribute += "{label:disp,value:str}";
                }
            }
            attribute = ".edge(n:\"" + attribute.substring(1, questionIdx).replace("\"", "\\\"") + "\"){"
                + attribute.substring(questionIdx + 1) + "}";
        }

        if (attribute.charAt(0) == '.') {
            return readDotAtt(alias, attribute.substring(1), innerAtts);
        }
        return readSimpleAtt(alias, attribute, innerAtts);
    }

    public AttWithProc readProcessors(String attribute) {

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

            if (!AttStrUtils.isInQuotes(orElsePart)) {

                if (orElsePart.isEmpty()) {
                    orElsePart = "''";
                } else if (!orElsePart.equals("null")
                        && !orElsePart.equals(Boolean.TRUE.toString())
                        && !orElsePart.equals(Boolean.FALSE.toString())
                        && !Character.isDigit(orElsePart.charAt(0))) {

                    orElsePart = "'"
                        + AttOrElseProcessor.ATT_PREFIX
                        + orElsePart.replace("'", "\\'")
                        + "'";
                }
            }

            att = att.substring(0, orElseDelimIdx) + "|or(" + orElsePart + ")"
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

        return new AttWithProc(att, processors);
    }

    /* === PRIVATE === */

    private SchemaRootAtt readRoot(String alias, String attribute) {

        if (StringUtils.isBlank(attribute)) {
            throw new AttReadException(alias, attribute, "Empty attribute");
        }

        AttWithProc attWithProc = readProcessors(attribute);
        String att = attWithProc.getAttribute();

        SchemaAtt schemaAtt = readInner(alias, att);
        return new SchemaRootAtt(schemaAtt, attWithProc.getProcessors());
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
            alias = NameUtils.unescape(att.substring(0, aliasDelimIdx));
            alias = removeQuotes(alias);
            att = att.substring(aliasDelimIdx + 1);
        }

        if (!dotContext) {
            att = removeQuotes(att);
        }

        if (alias.isEmpty() && multipleAtts) {
            if (dotContext) {
                int braceIdx = att.indexOf('{');
                alias = braceIdx > 0 ? att.substring(0, braceIdx) : att;
            } else {
                int questionIdx = att.indexOf('?');
                if (questionIdx > 0) {
                    alias = att.substring(0, questionIdx);
                } else {
                    alias = att;
                }
            }
        }
        att = att.trim();
        return parserFunc.apply(alias.trim(), dotContext ? "." + att : att);
    }

    private SchemaAtt readLastSimpleAtt(String alias, String attribute, List<SchemaAtt> innerAtts) {

        if (attribute.charAt(0) == '?') {
            attribute = attribute.substring(1);
            if (attribute.startsWith("has(")) {
                return parseHasAtt(alias, attribute);
            } else if (attribute.startsWith("as(")) {
                return parseAsAtt(alias, attribute);
            }
            return SchemaAtt.create()
                .setAlias(alias)
                .setName('?' + attribute)
                .build();
        }

        String att = attribute;
        int questionIdx = AttStrUtils.indexOf(att, "?");
        if (questionIdx != -1) {
            att = att.substring(0, questionIdx) + "{?" + att.substring(questionIdx + 1) + "}";
        }

        int arrIdx = AttStrUtils.indexOf(att, "[]");
        boolean isMultiple = false;
        if (arrIdx != -1) {
            isMultiple = true;
            att = att.substring(0, arrIdx) + att.substring(arrIdx + 2);
        }

        String attName = att;
        String innerAttsStr = null;
        int openBraceIdx = AttStrUtils.indexOf(attName, '{');
        if (openBraceIdx > -1) {

            attName = attName.substring(0, openBraceIdx);

            int closeBraceIdx = AttStrUtils.indexOf(att, '}');
            if (closeBraceIdx == -1) {
                closeBraceIdx = att.length();
            }

            innerAttsStr = att.substring(openBraceIdx + 1, closeBraceIdx);
        }

        List<SchemaAtt> resInnerAtts;
        if (innerAttsStr == null) {
            if (!innerAtts.isEmpty()) {
                resInnerAtts = innerAtts;
            } else {
                resInnerAtts = readInnerAtts("?disp", false, this::readInner);
            }
        } else {
            resInnerAtts = readInnerAtts(innerAttsStr, false, this::readInner);
        }

        return SchemaAtt.create()
            .setAlias(alias)
            .setName(removeQuotes(attName))
            .setMultiple(isMultiple)
            .setInner(resInnerAtts)
            .build();
    }

    private SchemaAtt readSimpleAtt(String alias, String attribute, List<SchemaAtt> innerAtts) {

        if (attribute.contains("?as(")) {
            Matcher matcher = GQL_SIMPLE_AS_PATTERN.matcher(attribute);
            while (matcher.find()) {
                attribute = attribute.replace(matcher.group(0), "._as." + matcher.group(2));
                matcher = GQL_SIMPLE_AS_PATTERN.matcher(attribute);
            }
        }

        List<String> dotPath = AttStrUtils.split(attribute, '.');

        String lastPathElem = dotPath.get(dotPath.size() - 1);

        SchemaAtt schemaAtt = readLastSimpleAtt(dotPath.size() == 1 ? alias : "", lastPathElem, innerAtts);

        for (int i = dotPath.size() - 2; i >= 0; i--) {

            String pathElem = dotPath.get(i);

            int arrIdx = AttStrUtils.indexOf(pathElem, "[]");
            boolean isMultiple = false;
            if (arrIdx != -1) {
                isMultiple = true;
                pathElem = pathElem.substring(0, arrIdx) + pathElem.substring(arrIdx + 2);
            }

            schemaAtt = SchemaAtt.create()
                .setAlias(i == 0 ? alias : "")
                .setName(removeQuotes(pathElem))
                .setMultiple(isMultiple)
                .setInner(schemaAtt)
                .build();
        }

        return schemaAtt;
    }

    /**
     * @param attribute - attribute without dot
     */
    private SchemaAtt readDotAtt(String alias, String attribute, List<SchemaAtt> innerAtts) {

        if (attribute.startsWith("att")) {
            return parseGqlAtt(alias, attribute, innerAtts);
        }
        if (attribute.startsWith("edge")) {
            return readEdgeAtt(alias, attribute);
        }

        int braceIdx = AttStrUtils.indexOf(attribute, '(');
        if (braceIdx == -1) {
            return SchemaAtt.create()
                .setAlias(alias)
                .setName('?' + attribute)
                .build();
        }

        if (attribute.startsWith("as")) {
            return parseAsAtt(alias, attribute);
        }

        if (attribute.startsWith("has")) {
            return parseHasAtt(alias, attribute);
        }

        throw new AttReadException(alias, attribute, "Unknown dot attribute");
    }

    private SchemaAtt parseAsAtt(String alias, String attribute) {

        Matcher matcher = GQL_AS_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            throw new AttReadException(alias, attribute, "Incorrect 'as' attribute");
        }

        String attName  = matcher.group(2);
        String attInner = matcher.group(3);

        return SchemaAtt.create()
            .setAlias(alias)
            .setName(RecordConstants.ATT_AS)
            .setInner(SchemaAtt.create()
                .setName(attName)
                .setInner(readInnerAtts(attInner, true, this::readInner)))
            .build();
    }

    private SchemaAtt parseHasAtt(String alias, String attribute) {

        Matcher matcher = GQL_HAS_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            throw new AttReadException(alias, attribute, "Incorrect 'has' attribute");
        }

        String attName = matcher.group(2);

        return SchemaAtt.create()
            .setAlias(alias)
            .setName(RecordConstants.ATT_HAS)
            .setInner(SchemaAtt.create()
                .setName(attName)
                .setInner(SchemaAtt.create().setName("?bool"))
            ).build();
    }

    private SchemaAtt readEdgeAtt(String alias, String attribute) {

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

        return SchemaAtt.create()
            .setAlias(alias)
            .setName(RecordConstants.ATT_EDGE)
            .setInner(SchemaAtt.create().setName(AttStrUtils.removeEscaping(attName)).setInner(schemaInnerAtts))
            .build();
    }

    private SchemaAtt parseEdgeInnerAtt(String alias, String attribute) {

        SchemaAtt.Builder attBuilder = SchemaAtt.create()
            .setAlias(alias)
            .setName(attribute);

        switch (attribute) {
            case "name":
            case "title":
            case "description":
            case "javaClass":
            case "editorKey":
            case "type":
                return attBuilder.setInner(SchemaAtt.create().setName("?str")
                ).build();
            case "protected":
            case "canBeRead":
            case "multiple":
            case "isAssoc":
                return attBuilder.setInner(SchemaAtt.create()
                    .setName("?bool")
                ).build();
        }

        int openBraceIdx = attribute.indexOf('{');
        int closeBraceIdx = attribute.lastIndexOf('}');
        if (openBraceIdx == -1 || closeBraceIdx == -1 || openBraceIdx > closeBraceIdx) {
            throw new AttSchemaException("Incorrect edge inner attribute: '" + attribute + "'");
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
                return attBuilder.setName(attWithoutInner)
                    .setMultiple(multiple)
                    .setInner(readInnerAtts(innerAtts, true, this::readInner))
                    .build();
        }

        throw new AttReadException(alias, attribute, "Unknown 'edge inner' attribute");
    }

    private SchemaAtt parseGqlAtt(String alias, String attribute, List<SchemaAtt> innerAtts) {

        List<SchemaAtt> gqlInnerAtts = Collections.emptyList();
        String attName = null;
        boolean multiple = attribute.startsWith("atts");

        Matcher matcher = GQL_ATT_PATTERN.matcher(attribute);
        if (!matcher.matches()) {
            if (innerAtts.size() > 0) {
                matcher = GQL_ATT_WITHOUT_INNER_PATTERN.matcher(attribute);
                if (matcher.matches()) {
                    attName = matcher.group(2);
                    gqlInnerAtts = innerAtts;
                }
            }
        } else {
            attName = matcher.group(2);
            gqlInnerAtts = readInnerAtts(matcher.group(3), true, (a, n) -> readInner(a, n, innerAtts));
        }
        if (attName == null || gqlInnerAtts == null || gqlInnerAtts.isEmpty()) {
            throw new AttReadException(alias, attribute, "Incorrect att attribute");
        }
        return SchemaAtt.create()
            .setAlias(alias)
            .setName(AttStrUtils.removeEscaping(attName))
            .setMultiple(multiple)
            .setInner(gqlInnerAtts)
            .build();
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

        AttStrUtils.split(argsStr, ",").stream()
            .map(String::trim)
            .map(arg -> {
                if (arg.isEmpty()) {
                    return DataValue.createStr("");
                }
                if (arg.equals("null")) {
                    return DataValue.NULL;
                } else if (Boolean.TRUE.toString().equals(arg)) {
                    return DataValue.TRUE;
                } else if (Boolean.FALSE.toString().equals(arg)) {
                    return DataValue.FALSE;
                } else if (Character.isDigit(arg.charAt(0))) {
                    return DataValue.create(Double.parseDouble(arg));
                } else {
                    arg = AttStrUtils.removeQuotes(arg);
                    return DataValue.createStr(AttStrUtils.removeEscaping(arg));
                }
            })
            .forEach(result::add);

        return result;
    }

    private String removeQuotes(String value) {

        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);

        if (first == last && first == '"' || first == '\'') {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }

}
