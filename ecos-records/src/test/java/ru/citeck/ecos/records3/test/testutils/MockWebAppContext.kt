package ru.citeck.ecos.records3.test.testutils

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.api.discovery.EcosDiscoveryClient
import ru.citeck.ecos.webapp.api.executor.EcosTaskExecutor
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProperties
import ru.citeck.ecos.webapp.api.scheduling.EcosScheduledTask
import ru.citeck.ecos.webapp.api.scheduling.EcosTaskScheduler
import ru.citeck.ecos.webapp.api.scheduling.trigger.Trigger
import ru.citeck.ecos.webapp.api.web.EcosWebClient
import ru.citeck.ecos.webapp.api.web.EcosWebController
import ru.citeck.ecos.webapp.api.web.EcosWebExecutor
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

open class MockWebAppContext(val appName: String = "test-app") : EcosWebAppContext {

    private val executor = Executors.newScheduledThreadPool(10)

    var webClientExecuteImpl: ((String, String, Any) -> Any)? = null

    override fun <T> doWhenWebAppReady(order: Float, action: () -> T): Promise<T> {
        return Promises.resolve(action.invoke())
    }

    override fun getProperties(): EcosWebAppProperties {
        return EcosWebAppProperties(appName, appName + ":" + UUID.randomUUID().toString())
    }

    override fun getTaskExecutor(type: String): EcosTaskExecutor {
        return object : EcosTaskExecutor {
            override fun <R> execute(taskInfo: () -> String, action: () -> R): Promise<R> {
                val future = CompletableFuture<R>()
                executor.execute {
                    future.complete(action.invoke())
                }
                return Promises.create(future)
            }
        }
    }

    override fun getTaskScheduler(type: String): EcosTaskScheduler {
        return object : EcosTaskScheduler {
            override fun schedule(taskInfo: () -> String, delay: Duration, action: () -> Unit): EcosScheduledTask {
                return ScheduledTask(
                    taskInfo,
                    executor.schedule({ action.invoke() }, delay.toMillis(), TimeUnit.MILLISECONDS)
                )
            }

            override fun schedule(taskInfo: () -> String, trigger: Trigger, action: () -> Unit): EcosScheduledTask {
                error("Not implemented")
            }

            override fun scheduleAtFixedRate(
                taskInfo: () -> String,
                initialDelay: Duration,
                period: Duration,
                action: () -> Unit
            ): EcosScheduledTask {
                return ScheduledTask(
                    taskInfo,
                    executor.scheduleAtFixedRate(
                        { action.invoke() },
                        initialDelay.toMillis(),
                        period.toMillis(),
                        TimeUnit.MILLISECONDS
                    )
                )
            }

            override fun scheduleByCron(taskInfo: () -> String, cron: String, action: () -> Unit): EcosScheduledTask {
                error("Not implemented")
            }

            override fun scheduleWithFixedDelay(
                taskInfo: () -> String,
                initialDelay: Duration,
                delay: Duration,
                action: () -> Unit
            ): EcosScheduledTask {
                return ScheduledTask(
                    taskInfo,
                    executor.scheduleWithFixedDelay(
                        { action.invoke() },
                        initialDelay.toMillis(),
                        delay.toMillis(),
                        TimeUnit.MILLISECONDS
                    )
                )
            }
        }
    }

    override fun getWebClient(): EcosWebClient {
        return object : EcosWebClient {
            override fun <R : Any> execute(
                targetApp: String,
                path: String,
                request: Any,
                respType: Class<R>
            ): Promise<R> {
                val executeImpl = webClientExecuteImpl ?: return Promises.reject(RuntimeException("Not implemented"))
                val future = CompletableFuture<R>()
                executor.execute {
                    val res = executeImpl.invoke(targetApp, path, request)
                    future.complete(Json.mapper.convert(res, respType) ?: error("Conversion error. Res: $res"))
                }
                return Promises.create(future)
            }
        }
    }

    override fun getDiscoveryClient(): EcosDiscoveryClient {
        return object : EcosDiscoveryClient {
            override fun <T> doWhenAppWillBeAvailable(appName: String, action: () -> T): Promise<T> {
                return Promises.resolve(action.invoke())
            }
            override fun isAppAvailable(appName: String): Boolean {
                return true
            }
        }
    }

    override fun getWebController(): EcosWebController {
        return object : EcosWebController {
            override fun registerExecutor(requestPath: String, executor: EcosWebExecutor<*>) {
                // do nothing
            }
        }
    }

    override fun isReady(): Boolean {
        return true
    }

    class ScheduledTask(
        private val info: () -> String,
        private val future: ScheduledFuture<*>
    ) : EcosScheduledTask {

        private val id = UUID.randomUUID()

        override fun cancel(): Promise<Boolean> {
            future.cancel(false)
            return Promises.resolve(true)
        }

        override fun cancel(timeout: Duration): Promise<Boolean> {
            future.cancel(true)
            return Promises.resolve(true)
        }

        override fun getInfo(): String {
            return info.invoke()
        }

        override fun getId(): UUID {
            return id
        }

        override fun isActive(): Boolean {
            return !future.isDone
        }

        override fun isCancelled(): Boolean {
            return future.isCancelled
        }

        override fun isRunning(): Boolean {
            return false
        }
    }
}
