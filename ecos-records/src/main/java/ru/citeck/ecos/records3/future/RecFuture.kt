package ru.citeck.ecos.records3.future

/**
 * A {@code RecFuture} represents the result of an asynchronous
 * computation. Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation. The result can only be retrieved using method
 * {@code get} when the computation has completed, blocking if
 * necessary until it is ready.
 */
interface RecFuture<T> {

    /**
     * If this method return true then method get should
     * return result of this future without any delay
     */
    fun isDone(): Boolean

    fun get(): T

    fun <K> then(action: (T) -> K): RecFuture<K>
}
