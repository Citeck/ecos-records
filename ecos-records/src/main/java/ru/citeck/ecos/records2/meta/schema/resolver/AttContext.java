package ru.citeck.ecos.records2.meta.schema.resolver;

import lombok.Getter;
import lombok.Setter;
import ru.citeck.ecos.records2.meta.schema.SchemaAtt;

import java.util.function.Function;

public class AttContext {

    private static final ThreadLocal<AttContext> current = new ThreadLocal<>();

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

    public static <T> T doInContext(Function<AttContext, T> action) {
        AttContext context = new AttContext();
        current.set(context);
        try {
            return action.apply(context);
        } finally {
            current.set(null);
        }
    }
}
