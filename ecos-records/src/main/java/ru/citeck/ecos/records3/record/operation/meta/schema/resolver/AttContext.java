package ru.citeck.ecos.records3.record.operation.meta.schema.resolver;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.schema.SchemaAtt;
import ru.citeck.ecos.records3.record.operation.meta.schema.write.AttSchemaWriter;
import ru.citeck.ecos.records3.record.operation.meta.util.AttStrUtils;

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

    public static String getCurrentSchemaAttAsStr() {
        AttContext attContext = current.get();
        return attContext.serviceFactory.getAttSchemaWriter().write(attContext.getSchemaAtt());
    }

    public static String getCurrentSchemaAttInnerStr() {
        String att = getCurrentSchemaAttAsStr().trim();
        return att.substring(AttStrUtils.indexOf(att, "{") + 1, att.length() - 1);
    }

    public static <T> T doInContext(RecordsServiceFactory serviceFactory, Function<AttContext, T> action) {
        AttContext context = new AttContext();
        context.serviceFactory = serviceFactory;
        current.set(context);
        try {
            return action.apply(context);
        } finally {
            current.set(null);
        }
    }
}
