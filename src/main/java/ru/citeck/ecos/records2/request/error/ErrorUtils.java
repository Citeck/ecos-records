package ru.citeck.ecos.records2.request.error;

public class ErrorUtils {

    public static RecordsError convertException(Exception exception) {

        StringBuilder sb = new StringBuilder();
        sb.append(exception.getLocalizedMessage());

        Throwable throwable = exception;

        if (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        if (exception != throwable) {
            sb.append("\n    Caused by ")
                .append(throwable.getClass().getSimpleName())
                .append(": ")
                .append(throwable.getLocalizedMessage());
        }

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            int lines = Math.min(stackTrace.length, 3);
            for (int i = 0; i < lines; i++) {
                StackTraceElement element = stackTrace[i];
                sb.append("\n    ").append(element.toString());
            }
        }

        return new RecordsError(throwable.getClass().getSimpleName(), sb.toString());
    }
}
