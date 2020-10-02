package ru.citeck.ecos.records3.record.request.error;

import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.commons.utils.MandatoryParam;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ErrorUtils {

    public static RecordError convertException(Throwable exception) {

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

        RecordError error = new RecordError();
        error.setType(throwable.getClass().getSimpleName());
        error.setMsg(throwable.getLocalizedMessage());

        return error;
    }

    /*public static boolean logErrors(RecordsResult<?> result) {

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
            msg.append("Req Body: ").append(body).append("\n");
            log.error(msg.toString());
            return true;
        }
        return false;
    }*/
}
