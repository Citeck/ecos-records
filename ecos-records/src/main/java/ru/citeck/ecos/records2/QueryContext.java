package ru.citeck.ecos.records2;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records3.record.request.RequestContext;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @deprecated -> RequestContext
 */
@Slf4j
@Deprecated
public class QueryContext {

    public static final String CTX_KEY = "__query-context";

    private List<?> metaValues;

    @Setter
    private RecordsServiceFactory serviceFactory;

    @SuppressWarnings("unchecked")
    public static <T extends QueryContext> T getCurrent() {
        RequestContext current = RequestContext.getCurrent();
        return current != null ? current.getVar(CTX_KEY) : null;
    }

    public boolean isComputedAttsDisabled() {
        return RequestContext.getCurrentNotNull().getCtxData().isComputedAttsDisabled();
    }

    public static <T> T withoutComputedAtts(Supplier<T> callable) {
        return RequestContext.doWithCtx(b -> b.setCompAttsDisabled(true), ctx -> callable.get());
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
        return RequestContext.doWithAtts(contextAttributes, callable);
    }

    public static void withContext(RecordsServiceFactory serviceFactory,
                                    Runnable runnable,
                                    Map<String, Object> atts) {

        withContext(serviceFactory, ctx ->
            RequestContext.doWithCtx(b -> b.setCtxAtts(atts), reqCtx -> {
                runnable.run();
                return null;
            })
        );
    }

    public static <T> T withContext(RecordsServiceFactory serviceFactory, Supplier<T> callable) {
        return withContext(serviceFactory, ctx -> callable.get());
    }

    public static <T> T withContext(RecordsServiceFactory serviceFactory, Function<QueryContext, T> callable) {

        return RequestContext.doWithCtx(serviceFactory, ctx -> {
            QueryContext qCtx = ctx.getOrPutVar(CTX_KEY, QueryContext.class, serviceFactory::createQueryContext);
            return callable.apply(qCtx);
        });
    }

    public Locale getLocale() {
        return RequestContext.getCurrentNotNull().getLocale();
    }

    public List<?> getMetaValues() {
        return metaValues;
    }

    public void setMetaValues(List<?> metaValues) {
        this.metaValues = metaValues;
    }

    public RecordsServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public RecordsService getRecordsService() {
        return serviceFactory.getRecordsService();
    }

    public Map<String, Object> getAttributes() {
        return RequestContext.getCurrentNotNull().getAttributes();
    }
}
