package ru.citeck.ecos.records3.utils.func;

@FunctionalInterface
public interface UnsafeSupplier<T> {

    T get() throws Exception;
}
