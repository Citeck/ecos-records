package ru.citeck.ecos.records2.request.error;

import ru.citeck.ecos.records2.utils.MandatoryParam;

import java.util.ArrayList;
import java.util.List;

public class ErrorUtils {

    public static RecordsError convertException(Exception exception) {

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
        error.setMsg(throwable.getLocalizedMessage());
        error.setStackTrace(errorStackTrace);

        return error;
    }
}
