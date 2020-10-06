package ru.citeck.ecos.records3.record.operation.meta.schema.resolver;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.write.AttSchemaWriter;
import ru.citeck.ecos.records3.record.operation.meta.util.AttStrUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AttContext {

    private static final ThreadLocal<AttContext> current = new ThreadLocal<>();

    private RecordsServiceFactory serviceFactory;

    private SchemaAtt schemaAtt;
    @Getter
    @Setter
    private boolean schemaAttWasRequested;

    public SchemaAtt getSchemaAtt() {
        schemaAttWasRequested = true;
        return schemaAtt;
    }

    public void setSchemaAtt(SchemaAtt schemaAtt) {
        this.schemaAtt = schemaAtt;
        schemaAttWasRequested = false;
    }

    public static SchemaAtt getCurrentSchemaAtt() {
        return current.get().getSchemaAtt();
    }

    public static Map<String, String> getInnerAttsMap() {

        AttContext attContext = current.get();
        AttSchemaWriter writer = attContext.serviceFactory.getAttSchemaWriter();

        Map<String, String> result = new HashMap<>();
        attContext.getSchemaAtt().getInner().forEach(att ->
            result.put(att.getName(), writer.write(att)));

        return result;
    }

    public static String getCurrentSchemaAttAsStr() {
        AttContext attContext = current.get();
        return attContext.serviceFactory.getAttSchemaWriter().write(attContext.getSchemaAtt());
    }

    public static String getCurrentSchemaAttInnerStr() {
        String att = getCurrentSchemaAttAsStr().trim();
        return att.substring(AttStrUtils.indexOf(att, "{") + 1, att.length() - 1);
    }

    @NotNull
    public static AttContext getCurrentNotNull() {
        AttContext context = current.get();
        if (context == null) {
            throw new IllegalStateException("Current att context is null");
        }
        return context;
    }

    @Nullable
    public static AttContext getCurrent() {
        return current.get();
    }

    public static <T> T doInContext(RecordsServiceFactory serviceFactory, Function<AttContext, T> action) {

        boolean isOwner = false;
        AttContext ctx = current.get();
        if (ctx == null) {
            ctx = new AttContext();
            ctx.serviceFactory = serviceFactory;
            current.set(ctx);
            isOwner = true;
        }

        try {
            return action.apply(ctx);
        } finally {
            if (isOwner) {
                current.set(null);
            }
        }
    }
}
