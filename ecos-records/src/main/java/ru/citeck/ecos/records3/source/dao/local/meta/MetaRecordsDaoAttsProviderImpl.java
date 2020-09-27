package ru.citeck.ecos.records3.source.dao.local.meta;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetaRecordsDaoAttsProviderImpl implements MetaRecordsDaoAttsProvider {

    private final Map<String, MetaAttributesSupplier> suppliers = new ConcurrentHashMap<>();

    @Override
    public Object getAttributes() {
        return new Attributes();
    }

    @Override
    public void register(MetaAttributesSupplier supplier) {
        supplier.getAttributesList().forEach(a -> suppliers.put(a, supplier));
    }

    public class Attributes implements AttValue {

        @Override
        public Object getAttribute(@NotNull String name) throws Exception {
            MetaAttributesSupplier supplier = suppliers.get(name);
            if (supplier == null) {
                return null;
            }
            return supplier.getAttribute(name);
        }
    }
}
