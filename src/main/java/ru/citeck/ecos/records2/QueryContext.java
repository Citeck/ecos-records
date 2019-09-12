package ru.citeck.ecos.records2;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class QueryContext {

    private static final ThreadLocal<QueryContext> current = new ThreadLocal<>();

    private List<?> metaValues;
    private Map<String, Object> contextData = new ConcurrentHashMap<>();

    private final RecordsServiceFactory serviceFactory;

    public QueryContext(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @SuppressWarnings("unchecked")
    public static <T extends QueryContext> T getCurrent() {
        return (T) current.get();
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

    public RecordsService getRecordsService() {
        return serviceFactory.getRecordsService();
    }
}
