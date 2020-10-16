package ru.citeck.ecos.records3.record.op.atts.service.value.factory;

import ru.citeck.ecos.records3.record.op.atts.service.value.AttValue;

import java.util.List;

public interface AttValueFactory<T> {

    AttValue getValue(T value);

    List<Class<? extends T>> getValueTypes();
}
