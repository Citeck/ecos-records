package ru.citeck.ecos.records3.record.request;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.records3.record.request.msg.MsgType;
import ru.citeck.ecos.records3.record.request.msg.RequestMsg;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class RequestContext {

    private static final ThreadLocal<RequestContext> current = new ThreadLocal<>();
    private static final Map<Class<?>, String> msgTypeByClass = new ConcurrentHashMap<>();

    private final Map<String, Object> ctxVars = new ConcurrentHashMap<>();

    private RequestCtxData<?> ctxData;
    private RecordsServiceFactory serviceFactory;

    private final List<RequestMsg> messages = new ArrayList<>();

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends RequestContext> T getCurrent() {
        return (T) current.get();
    }

    @NotNull
    public static <T extends RequestContext> T getCurrentNotNull() {
        T context = getCurrent();
        if (context == null) {
            throw new IllegalStateException("Request context is mandatory. " +
                "Add RequestContext.withCtx(ctx -> {...}) before call");
        }
        return context;
    }

    public static <T> T withAtts(@NotNull Map<String, Object> atts,
                                 @NotNull Function<RequestContext, T> action) {
        return withCtx(null, b -> b.setCtxAtts(atts), action);
    }

    public static <T> T withAtts(@NotNull Map<String, Object> atts,
                                 @NotNull Supplier<T> action) {
        return withCtx(null, b -> b.setCtxAtts(atts), ctx -> action.get());
    }

    public static <T> T withCtx(@NotNull Function<RequestContext, T> action) {
        return withCtx(null, action);
    }

    public static <T> T withCtx(@Nullable RecordsServiceFactory factory,
                                @NotNull Function<RequestContext, T> action) {

        return withCtx(factory, null, action);
    }
    public static <T> T withCtx(@Nullable RecordsServiceFactory factory,
                                @Nullable Consumer<RequestCtxData.Builder<T>> ctxData,
                                @NotNull Function<RequestContext, T> action) {

        RequestContext current = RequestContext.getCurrent();

        if (factory == null && (current == null || current.serviceFactory == null)) {
            throw new IllegalStateException("RecordsServiceFactory is not found in context!");
        }

        boolean isContextOwner = false;

        RequestCtxData<T> prevCtxData = null;

        if (current == null) {

            RequestCtxData.Builder<T> builder = RequestCtxData.create();
            if (ctxData != null) {
                ctxData.accept(builder);
            }

            current = factory.createRequestContext();
            current.ctxData = builder.build();
            current.serviceFactory = factory;

            RequestContext.setCurrent(current);
            isContextOwner = true;

        } else {

            @SuppressWarnings("unchecked")
            RequestCtxData<T> prevCtxDataT = (RequestCtxData<T>) current.ctxData;
            prevCtxData = prevCtxDataT;

            if (ctxData != null) {

                RequestCtxData.Builder<T> builder = prevCtxData.modify();
                ctxData.accept(builder);

                current.ctxData = prevCtxData.modify().merge(builder.build()).build();
            }
        }

        try {
            return action.apply(current);
        } finally {
            current.ctxData = prevCtxData;
            if (isContextOwner) {
                current.getMessages().forEach(msg -> {
                    switch (msg.getLevel()) {
                        case ERROR:
                            log.error(msg.toString());
                            break;
                        case WARN:
                            log.warn(msg.toString());
                            break;
                        case INFO:
                            log.info(msg.toString());
                            break;
                        case DEBUG:
                            log.debug(msg.toString());
                            break;
                        case TRACE:
                            log.trace(msg.toString());
                            break;
                        default:
                            log.warn("Unknown msg type: " + msg);
                    }
                });
                RequestContext.removeCurrent();
            }
        }
    }

    public Locale getLocale() {
        return ctxData.getLocale();
    }

    public static <T extends RequestContext> void setCurrent(T context) {
        current.set(context);
    }

    public static void removeCurrent() {
        current.remove();
    }

    public boolean hasVar(String key) {
        return ctxVars.containsKey(key);
    }

    public void putVar(String key, Object data) {
        ctxVars.put(key, data);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVar(String key) {
        return (T) ctxVars.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeVar(String key) {
        return (T) ctxVars.remove(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrPutVar(String key, Class<?> type, Supplier<? extends T> newValue) {
        Object value = ctxVars.computeIfAbsent(key, k -> newValue.get());
        if (!type.isInstance(value)) {
            log.warn("Context data with the key '" + key + "' is not an instance of a " + type + ". "
                      + "Data will be overridden. Current data: " + value);
            value = newValue.get();
            ctxVars.put(key, value);
        }
        return (T) value;
    }

    public <K, V> Map<K, V> getMap(String key) {
        return getOrPutVar(key, Map.class, HashMap::new);
    }

    public <T> List<T> getList(String key) {
        return getOrPutVar(key, List.class, ArrayList::new);
    }

    public <T> Set<T> getSet(String key) {
        return getOrPutVar(key, Set.class, HashSet::new);
    }

    public int getCount(String key) {
        return getOrPutVar(key, AtomicInteger.class, AtomicInteger::new).get();
    }

    public int incrementCount(String key) {
        return getOrPutVar(key, AtomicInteger.class, AtomicInteger::new).incrementAndGet();
    }

    public int decrementCount(String key) {
        return this.decrementCount(key, true);
    }

    public int decrementCount(String key, boolean allowNegative) {
        AtomicInteger counter = getOrPutVar(key, AtomicInteger.class, AtomicInteger::new);
        if (allowNegative || counter.get() > 0) {
            return counter.decrementAndGet();
        }
        return counter.get();
    }

    public RecordsServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public RecordsService getRecordsService() {
        return serviceFactory.getRecordsService();
    }

    public Map<String, Object> getAttributes() {
        return ctxData.getCtxAtts();
    }

    public void clearMessages() {
        this.messages.clear();
    }

    public List<RequestMsg> getMessages() {
        return messages;
    }

    public RequestCtxData<?> getCtxData() {
        return ctxData;
    }

    public boolean isMsgEnabled(MsgLevel level) {
        return ctxData.getMsgLevel().isEnabled(level);
    }

    public void addMsg(MsgLevel level, Supplier<Object> msg) {

        if (!level.isAllowedForMsg()) {
            log.error("You can't add message with level " + level + ". Msg: " + msg.get());
            return;
        }

        if (!isMsgEnabled(level)) {
            return;
        }

        Object msgValue = msg.get();
        if (msgValue == null) {
            msgValue = "null";
        }

        String type;

        if (msgValue instanceof String) {
            type = "text";
        } else {
            type = getMessageTypeByClass(msgValue.getClass());
        }

        messages.add(new RequestMsg(
            level,
            Instant.now(),
            type,
            DataValue.create(msgValue),
            ctxData.getQueryTrace()
        ));
    }

    private String getMessageTypeByClass(Class<?> clazz) {

        if (clazz == String.class) {
            return "text";
        }

        return msgTypeByClass.computeIfAbsent(clazz, c -> {
            MsgType annotation = c.getAnnotation(MsgType.class);
            return annotation != null ? annotation.value() : "any";
        });
    }

    public List<RequestMsg> getErrors() {
        return messages.stream()
            .filter(m -> MsgLevel.ERROR.isEnabled(m.getLevel()))
            .collect(Collectors.toList());
    }

    public void setServiceFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }
}
