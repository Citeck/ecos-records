package ru.citeck.ecos.records2;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class QueryContext {

    private static final ThreadLocal<QueryContext> current = new ThreadLocal<>();

    private List<?> metaValues;
    private final Map<String, Object> contextData = new ConcurrentHashMap<>();
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Getter
    private boolean computedAttsDisabled;

    private Locale locale = Locale.ENGLISH;

    private RecordsServiceFactory serviceFactory;

    @SuppressWarnings("unchecked")
    public static <T extends QueryContext> T getCurrent() {
        return (T) current.get();
    }

    public static <T> T withoutComputedAtts(Supplier<T> callable) {

        QueryContext context = QueryContext.getCurrent();

        if (context == null) {
            log.warn("Query context is not defined! " + callable);
            return callable.get();
        }

        boolean valueBefore = context.computedAttsDisabled;
        context.computedAttsDisabled = true;

        try {
            return callable.get();
        } finally {
            context.computedAttsDisabled = valueBefore;
        }
    }

    public static void withAttributes(Map<String, Object> contextAttributes, Runnable runnable) {
        withAttributes(contextAttributes, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T withAttributes(Map<String, Object> contextAttributes, Supplier<T> callable) {

        QueryContext context = QueryContext.getCurrent();
        if (context == null) {
            throw new IllegalStateException("Query context is not defined! Attributes: " + contextAttributes);
        }

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> contextAtts = context.attributes;
        contextAttributes.forEach((k, v) -> {
            before.put(k, contextAtts.get(k));
            contextAtts.put(k, v);
        });

        try {
            return callable.get();
        } finally {
            before.forEach((k, v) -> {
                if (v != null) {
                    contextAtts.put(k, v);
                } else {
                    contextAtts.remove(k);
                }
            });
        }
    }

    public static <T> T withContext(RecordsServiceFactory serviceFactory,
                                    Supplier<T> callable,
                                    Map<String, Object> contextAttributes) {

        return withContext(serviceFactory, currentCtx -> {

            currentCtx.attributes.putAll(contextAttributes);
            try {
                return callable.get();
            } finally {
                currentCtx.attributes.clear();
            }
        });
    }

    public static void withContext(RecordsServiceFactory serviceFactory,
                                    Runnable runnable,
                                    Map<String, Object> contextAttributes) {

        withContext(serviceFactory, currentCtx -> {

            currentCtx.attributes.putAll(contextAttributes);
            try {
                runnable.run();
            } finally {
                currentCtx.attributes.clear();
            }
            return null;
        });
    }

    public static <T> T withContext(RecordsServiceFactory serviceFactory, Supplier<T> callable) {
        return withContext(serviceFactory, ctx -> callable.get());
    }

    public static <T> T withContext(RecordsServiceFactory serviceFactory, Function<QueryContext, T> callable) {

        QueryContext context = QueryContext.getCurrent();
        boolean isContextOwner = false;
        if (context == null) {
            context = serviceFactory.createQueryContext();
            QueryContext.setCurrent(context);
            isContextOwner = true;
        }

        T result;

        try {
            result = callable.apply(context);
        } finally {
            if (isContextOwner) {
                QueryContext.removeCurrent();
            }
        }

        return result;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale != null ? locale : Locale.ENGLISH;
    }

    public static <T extends QueryContext> void setCurrent(T context) {
        current.set(context);
    }

    public static void removeCurrent() {
        current.remove();
    }

    public boolean hasData(String key) {
        return contextData.containsKey(key);
    }

    public void putData(String key, Object data) {
        contextData.put(key, data);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) contextData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeData(String key) {
        return (T) contextData.remove(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrPutData(String key, Class<?> type, Supplier<? extends T> newValue) {
        Object value = contextData.computeIfAbsent(key, k -> newValue.get());
        if (!type.isInstance(value)) {
            log.warn("Context data with the key '" + key + "' is not an instance of a " + type + ". "
                      + "Data will be overridden. Current data: " + value);
            value = newValue.get();
            contextData.put(key, value);
        }
        return (T) value;
    }

    public <K, V> Map<K, V> getMap(String key) {
        return getOrPutData(key, Map.class, HashMap::new);
    }

    public <T> List<T> getList(String key) {
        return getOrPutData(key, List.class, ArrayList::new);
    }

    public <T> Set<T> getSet(String key) {
        return getOrPutData(key, Set.class, HashSet::new);
    }

    public int getCount(String key) {
        return getOrPutData(key, AtomicInteger.class, AtomicInteger::new).get();
    }

    public int incrementCount(String key) {
        return getOrPutData(key, AtomicInteger.class, AtomicInteger::new).incrementAndGet();
    }

    public int decrementCount(String key) {
        return this.decrementCount(key, true);
    }

    public int decrementCount(String key, boolean allowNegative) {
        AtomicInteger counter = getOrPutData(key, AtomicInteger.class, AtomicInteger::new);
        if (allowNegative || counter.get() > 0) {
            return counter.decrementAndGet();
        }
        return counter.get();
    }

    public List<?> getMetaValues() {
        return metaValues;
    }

    public void setMetaValues(List<?> metaValues) {
        this.metaValues = metaValues;
    }

    public void setServiceFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public RecordsServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public RecordsService getRecordsService() {
        return serviceFactory.getRecordsService();
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
