package ru.citeck.ecos.records3.graphql.meta.value.factory;

import ru.citeck.ecos.records3.graphql.meta.value.MetaValue;

import java.util.List;

public interface MetaValueFactory<T> {

    MetaValue getValue(T value);

    List<Class<? extends T>> getValueTypes();
}
