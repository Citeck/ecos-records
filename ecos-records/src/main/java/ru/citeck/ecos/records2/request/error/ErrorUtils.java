package ru.citeck.ecos.records2.request.error;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.exception.ExceptionMessageExtractor;
import ru.citeck.ecos.records3.security.HasSensitiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ErrorUtils {

    public static RecordsError convertException(Throwable exception, @Nullable RecordsServiceFactory services) {

        MandatoryParam.check("exception", exception);

        Throwable throwable = exception;

        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        List<String> errorStackTrace = new ArrayList<>();
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (int i = 0; i < 3 && i < stackTrace.length; i++) {
                errorStackTrace.add(String.valueOf(stackTrace[i]));
            }
        }

        RecordsError error = new RecordsError();
        error.setType(throwable.getClass().getSimpleName());
        error.setMsg(extractMessage(throwable, services));
        error.setStackTrace(errorStackTrace);

        return error;
    }

    public static boolean logErrors(RecordsResult<?> result) {

        StringBuilder sb = new StringBuilder();
        if (logErrors(result, sb)) {
            log.error(sb.toString());
            return true;
        }
        return false;
    }

    public static boolean logErrors(RecordsResult<?> result, StringBuilder msg) {

        if (result.getErrors().isEmpty()) {
            return false;
        }

        msg.append("Records request finished with ERRORS\n");

        result.getErrors().forEach(err -> {

            msg.append("ERROR: [")
                .append(err.getType())
                .append("] ")
                .append(err.getMsg())
                .append("\n");

            err.getStackTrace().forEach(line -> msg.append(line).append("\n"));
        });

        return true;
    }

    public static boolean logErrorsWithBody(RecordsResult<?> result, Object body) {

        StringBuilder msg = new StringBuilder();
        if (logErrors(result, msg)) {
            if (body instanceof HasSensitiveData<?>) {
                body = ((HasSensitiveData<?>) body).withoutSensitiveData();
            }
            msg.append("Req Body: ").append(body).append("\n");
            log.error(msg.toString());
            return true;
        }
        return false;
    }

    private static String extractMessage(Throwable exception, RecordsServiceFactory services) {
        if (exception == null) {
            return "null";
        }
        if (services != null) {
            Map<Class<? extends Throwable>, ExceptionMessageExtractor<Throwable>> extractors =
                services.getExceptionMessageExtractors();

            for (Class<? extends Throwable> type : extractors.keySet()) {
                if (type.isInstance(exception)) {
                    String message = extractors.get(type).getMessage(exception);
                    if (message != null) {
                        return message;
                    }
                }
            }
        }
        return exception.getLocalizedMessage();
    }
}
