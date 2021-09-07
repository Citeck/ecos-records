package ru.citeck.ecos.records2.rest;

import ru.citeck.ecos.context.lib.auth.AuthContext;

/**
 * Use AuthContext instead
 */
@Deprecated
public class RemoteRecordsUtils {

    public static <T> T runAsSystem(Action<T> action) {
        return AuthContext.runAsSystem(action::execute);
    }

    public static boolean isSystemContext() {
        return AuthContext.isRunAsSystem();
    }

    public interface Action<T> {

        T execute();
    }
}
