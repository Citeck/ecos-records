package ru.citeck.ecos.records3.record.op.atts.value.factory;

import ru.citeck.ecos.records3.record.op.atts.value.AttValue;

import java.util.List;

public interface AttValueFactory<T> {

    AttValue getValue(T value);

    List<Class<? extends T>> getValueTypes();
}
