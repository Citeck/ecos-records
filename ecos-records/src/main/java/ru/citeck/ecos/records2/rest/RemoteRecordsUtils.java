package ru.citeck.ecos.records2.rest;

import ru.citeck.ecos.records3.record.request.context.SystemContextUtil;

public class RemoteRecordsUtils {

    private static final ThreadLocal<Boolean> isSystem = new ThreadLocal<>();

    public static <T> T runAsSystem(Action<T> action) {
        isSystem.set(true);
        try {
            return action.execute();
        } finally {
            isSystem.remove();
        }
    }

    public static boolean isSystemContext() {
        return Boolean.TRUE.equals(isSystem.get()) || SystemContextUtil.isSystemContext();
    }

    public interface Action<T> {

        T execute();
    }
}
