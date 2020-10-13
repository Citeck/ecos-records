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
        error.setStackTrace(errorStackTrace);

        return error;
    }
}
