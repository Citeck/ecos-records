package ru.citeck.ecos.records3.test.testutils

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.promise.Promises
import ru.citeck.ecos.webapp.api.apps.EcosWebAppsApi
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.api.promise.Promise
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProperties
import ru.citeck.ecos.webapp.api.task.EcosTasksApi
import ru.citeck.ecos.webapp.api.task.executor.EcosTaskExecutor
import ru.citeck.ecos.webapp.api.task.scheduler.EcosScheduledTask
import ru.citeck.ecos.webapp.api.task.scheduler.EcosTaskScheduler
import ru.citeck.ecos.webapp.api.task.scheduler.trigger.Trigger
import ru.citeck.ecos.webapp.api.web.EcosWebClient
import ru.citeck.ecos.webapp.api.web.EcosWebController
import ru.citeck.ecos.webapp.api.web.EcosWebExecutor
import java.time.Duration
import java.util.*
import java.util.concurrent.*

open class WebAppContextMock(
    val appName: String = "test-app",
    val gatewayMode: Boolean = false
) : EcosWebAppContext {

    private val executor = Executors.newScheduledThreadPool(1)

    private val tasksApi = object : EcosTasksApi {

        override fun getTaskExecutor(key: String): EcosTaskExecutor {
            return object : EcosTaskExecutor {
                override fun <R> execute(taskInfo: () -> String, task: () -> R): Promise<R> {
                    val future = CompletableFuture<R>()
                    val callException = RuntimeException("task execution source")
                    executor.execute {
                        try {
                            future.complete(task.invoke())
                        } catch (e: Throwable) {
                            e.addSuppressed(callException)
                            future.completeExceptionally(e)
                        }
                    }
                    return Promises.create(future)
                }
                override fun getAsJavaExecutor(): Executor {
                    return executor
                }
            }
        }

        override fun getTaskScheduler(key: String): EcosTaskScheduler {
            return object : EcosTaskScheduler {
                override fun schedule(taskInfo: () -> String, delay: Duration, task: () -> Unit): EcosScheduledTask {
                    return ScheduledTask(
                        taskInfo,
                        executor.schedule({ task.invoke() }, delay.toMillis(), TimeUnit.MILLISECONDS)
                    )
                }

                override fun schedule(taskInfo: () -> String, trigger: Trigger, task: () -> Unit): EcosScheduledTask {
                    error("Not implemented")
                }

                override fun scheduleAtFixedRate(
                    taskInfo: () -> String,
                    initialDelay: Duration,
                    period: Duration,
                    task: () -> Unit
                ): EcosScheduledTask {
                    return ScheduledTask(
                        taskInfo,
                        executor.scheduleAtFixedRate(
                            { task.invoke() },
                            initialDelay.toMillis(),
                            period.toMillis(),
                            TimeUnit.MILLISECONDS
                        )
                    )
                }

                override fun scheduleByCron(taskInfo: () -> String, cron: String, task: () -> Unit): EcosScheduledTask {
                    error("Not implemented")
                }

                override fun scheduleWithFixedDelay(
                    taskInfo: () -> String,
                    initialDelay: Duration,
                    delay: Duration,
                    task: () -> Unit
                ): EcosScheduledTask {
                    return ScheduledTask(
                        taskInfo,
                        executor.scheduleWithFixedDelay(
                            { task.invoke() },
                            initialDelay.toMillis(),
                            delay.toMillis(),
                            TimeUnit.MILLISECONDS
                        )
                    )
                }

                override fun getAsJavaScheduledExecutor(): ScheduledExecutorService {
                    return executor
                }
            }
        }
    }

    private val webClientExecutor = tasksApi.getTaskExecutor("web-client")
    var webClientExecuteImpl: ((String, String, Any) -> Any)? = null

    override fun <T> doWhenAppReady(order: Float, action: () -> T): Promise<T> {
        return Promises.resolve(action.invoke())
    }

    override fun getProperties(): EcosWebAppProperties {
        return EcosWebAppProperties(appName, appName + ":" + UUID.randomUUID().toString(), gatewayMode)
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
                return webClientExecutor.execute({ "request $targetApp $path $request $respType" }) {
                    val res = executeImpl.invoke(targetApp, path, request)
                    Json.mapper.convert(res, respType) ?: error("Conversion error. Res: $res")
                }
            }
        }
    }

    override fun getTasksApi(): EcosTasksApi {
        return tasksApi
    }

    override fun getWebAppsApi(): EcosWebAppsApi {
        return object : EcosWebAppsApi {
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
