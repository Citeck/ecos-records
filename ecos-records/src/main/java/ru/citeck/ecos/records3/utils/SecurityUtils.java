package ru.citeck.ecos.records3.utils;

import ru.citeck.ecos.records3.record.error.RecordsError;
import ru.citeck.ecos.records3.request.result.RecordsResult;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SecurityUtils {

    private static final Pattern CLASS_PATTERN = Pattern.compile("([a-z0-9]+\\.)+[A-Z][a-zA-Z0-9]*");
    private static final Pattern CLASS_LINE_PATTERN = Pattern.compile("\\([a-zA-Z0-9]+\\.java:(\\d+)\\)");

    public static <T> RecordsResult<T> encodeResult(RecordsResult<T> result) {

        for (RecordsError error : result.getErrors()) {
            error.setMsg(encodeClasses(error.getMsg()));

            List<String> stackTrace = error.getStackTrace();
            if (stackTrace != null && !stackTrace.isEmpty()) {
                error.setStackTrace(stackTrace
                    .stream()
                    .map(SecurityUtils::encodeClasses)
                    .collect(Collectors.toList()));
            }
        }

        return result;
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
