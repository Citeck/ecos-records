package ru.citeck.ecos.records3.record.op.atts.proc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.List;

public abstract class AbstractAttProcessor<T> implements AttProcessor {

    private boolean allowNull;

    public AbstractAttProcessor(boolean allowNull) {
        this.allowNull = allowNull;
    }

    public AbstractAttProcessor() {
    }

    @NotNull
    public Object process(@NotNull ObjectData meta, @NotNull DataValue value, @NotNull List<DataValue> arguments) {

        if (!allowNull && value.isNull()) {
            return value;
        }

        T args = parseArgs(arguments);

        if (value.isArray()) {
            DataValue res = DataValue.createArr();
            value.forEach(v -> res.add(processOne(meta, v, args)));
            return res;
        } else {
            return processOne(meta, value, args);
        }
    }

    protected abstract Object processOne(@NotNull ObjectData meta, @NotNull DataValue value, @NotNull T arguments);

    protected abstract T parseArgs(@NotNull List<DataValue> arguments);
}
