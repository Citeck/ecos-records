package ru.citeck.ecos.records2.spring.utils;

public class RemoteRecordsUtils {

    private static ThreadLocal<Boolean> isSystem = new ThreadLocal<>();

    public static <T> T runAsSystem(Action<T> action) {
        isSystem.set(true);
        try {
            return action.execute();
        } finally {
            isSystem.remove();
        }
    }

    public static boolean isSystemContext() {
        return Boolean.TRUE.equals(isSystem.get());
    }

    public interface Action<T> {

        T execute();
    }
}
