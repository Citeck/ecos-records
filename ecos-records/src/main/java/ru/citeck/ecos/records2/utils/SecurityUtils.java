package ru.citeck.ecos.records2.utils;

import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.request.error.RecordsError;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.record.request.msg.ReqMsg;
import ru.citeck.ecos.records3.rest.v1.RequestResp;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SecurityUtils {

    private static final Pattern CLASS_PATTERN = Pattern.compile("([a-z0-9]+\\.)+[A-Z][a-zA-Z0-9]*");
    private static final Pattern CLASS_LINE_PATTERN = Pattern.compile("\\([a-zA-Z0-9]+\\.java:(\\d+)\\)");

    public static void encodeResult(RequestResp result) {

        List<ReqMsg> messages = result.getMessages();
        if (messages.isEmpty()) {
            return;
        }

        result.setMessages(messages.stream().map(m -> {
            if (!RecordsError.MSG_TYPE.equals(m.getType())) {
                return m;
            }
            RecordsError error = Json.getMapper().convert(m.getMsg(), RecordsError.class);
            return m.copy().withMsg(DataValue.create(error)).build();
        }).collect(Collectors.toList()));

    }

    public static <T> RecordsResult<T> encodeResult(RecordsResult<T> result) {

        result.setErrors(result.getErrors()
            .stream()
            .map(SecurityUtils::encodeError)
            .collect(Collectors.toList()));

        return result;
    }

    public static RecordsError encodeError(RecordsError error) {

        if (error == null) {
            return null;
        }

        error.setMsg(encodeClasses(error.getMsg()));

        List<String> stackTrace = error.getStackTrace();
        if (stackTrace != null && !stackTrace.isEmpty()) {
            error.setStackTrace(stackTrace
                .stream()
                .map(SecurityUtils::encodeClasses)
                .collect(Collectors.toList()));
        }

        return error;
    }

    private static String encodeClasses(String str) {

        if (str == null) {
            return null;
        }

        Matcher matcher = CLASS_PATTERN.matcher(str);
        String resultStr = str;

        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {

            String className = matcher.group(0);
            String[] packageAndClass = className.split("\\.");
            if (packageAndClass.length < 2) {
                continue;
            }

            builder.setLength(0);
            for (int i = 0; i < packageAndClass.length - 1; i++) {
                builder.append(packageAndClass[i].charAt(0));
            }

            String classShortName = packageAndClass[packageAndClass.length - 1];
            builder.append(classShortName.replaceAll("[a-z]", ""));

            resultStr = resultStr.replace(className, builder.toString());
        }

        matcher = CLASS_LINE_PATTERN.matcher(resultStr);
        while (matcher.find()) {
            resultStr = resultStr.replace(matcher.group(0), matcher.group(1));
        }

        return resultStr;
    }
}
