package ru.citeck.ecos.records2;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class QueryContext {

    private static final ThreadLocal<QueryContext> current = new ThreadLocal<>();

    private List<?> metaValues;
    private final Map<String, Object> contextData = new ConcurrentHashMap<>();

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

    public static <T> T withContext(RecordsServiceFactory serviceFactory, Supplier<T> callable) {

        QueryContext context = QueryContext.getCurrent();
        boolean isContextOwner = false;
        if (context == null) {
            context = serviceFactory.createQueryContext();
            QueryContext.setCurrent(context);
            isContextOwner = true;
        }

        T result;

        try {
            result = callable.get();
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
}
