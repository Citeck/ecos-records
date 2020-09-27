package ru.citeck.ecos.records3.record.operation.meta.value.factory;

import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;

import java.util.List;

public interface AttValueFactory<T> {

    AttValue getValue(T value);

    List<Class<? extends T>> getValueTypes();
}
