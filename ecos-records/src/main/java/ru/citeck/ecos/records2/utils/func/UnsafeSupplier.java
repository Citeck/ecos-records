package ru.citeck.ecos.records2.utils.func;

@FunctionalInterface
public interface UnsafeSupplier<T> {

    T get() throws Exception;
}
