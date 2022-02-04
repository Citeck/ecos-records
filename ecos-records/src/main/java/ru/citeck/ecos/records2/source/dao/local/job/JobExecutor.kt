package ru.citeck.ecos.records2.source.dao.local.job

import lombok.Data
import mu.KotlinLogging
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import java.lang.Exception
import java.lang.InterruptedException
import java.lang.Thread
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate

class JobExecutor(private val serviceFactory: RecordsServiceFactory) {

    companion object {
        private const val SYSTEM_JOBS_SOURCE_ID = "SYSTEM"

        private val log = KotlinLogging.logger {}
    }

    private val jobs: MutableList<JobInstance> = CopyOnWriteArrayList()
    private lateinit var executor: ScheduledExecutorService

    private var initialized = false

    fun isInitialized(): Boolean {
        return initialized
    }

    @Synchronized
    fun init() {
        if (initialized) {
            return
        }
        val corePoolSize = serviceFactory.properties.jobs.corePoolSize ?: 5

        if (corePoolSize > 0) {

            log.info { "Initialize JobExecutor with corePoolSize: $corePoolSize" }
            executor = Executors.newScheduledThreadPool(corePoolSize)

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    log.info("Shutdown hook triggered")
                    jobs.forEach(Consumer { it.enabled = false })
                    removeJobs { true }
                    executor.shutdown()
                    log.info("Shutdown hook completed")
                }
            )
            for (job in jobs) {
                scheduleJob(job)
            }
        } else {
            log.info { "JobExecutor disabled because corePoolSize is less or equal to zero" }
        }
        initialized = true
    }

    private fun scheduleJob(instance: JobInstance) {
        instance.future = if (instance.job is PeriodicJob) {
            executor.scheduleAtFixedRate(
                { execute(instance) },
                instance.job.getInitDelay(),
                instance.job.period,
                TimeUnit.MILLISECONDS
            )
        } else {
            executor.schedule(
                { execute(instance) },
                instance.job.initDelay,
                TimeUnit.MILLISECONDS
            )
        }
    }

    @Synchronized
    fun addJobs(sourceId: String, jobs: List<Job>) {
        jobs.forEach(Consumer { j: Job -> addJob(sourceId, j) })
    }

    @Synchronized
    fun addSystemJob(job: Job) {
        addJob(SYSTEM_JOBS_SOURCE_ID, job)
    }

    @Synchronized
    fun addJob(sourceId: String, job: Job) {
        val instance = JobInstance(sourceId, job)
        jobs.add(instance)
        if (initialized) {
            scheduleJob(instance)
        }
    }

    fun removeJobs(sourceId: String?) {
        removeJobs { it.sourceId == sourceId }
    }

    @Synchronized
    private fun removeJobs(filter: Predicate<JobInstance>) {
        jobs.removeIf { instance: JobInstance ->
            if (!filter.test(instance)) {
                return@removeIf false
            }
            instance.enabled = false
            if (!initialized || instance.future == null) {
                return@removeIf true
            }
            try {
                if (instance.running) {
                    val waitUntilRunning = System.currentTimeMillis() + 5000
                    log.info(
                        "Job from sourceId '" + instance.sourceId + "' is running. Try to wait until " +
                            Instant.ofEpochMilli(waitUntilRunning)
                    )
                    while (instance.running && System.currentTimeMillis() < waitUntilRunning) {
                        Thread.sleep(100)
                    }
                    if (instance.running) {
                        log.warn("Job is still running and will be cancelled")
                    } else {
                        log.info("Job is not running")
                    }
                }
                instance.future?.cancel(true)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                log.warn("Exception while job cancelling. SourceId: " + instance.sourceId, e)
            }
            true
        }
    }

    private fun execute(job: JobInstance) {
        if (!job.enabled) {
            return
        }
        job.running = true
        try {
            var count = 0
            while (count++ < 20) {
                val execResult = RequestContext.doWithCtx(serviceFactory) {
                    AuthContext.runAsSystem {
                        job.job.execute()
                    }
                }
                if (!execResult) {
                    break
                }
                if (!job.enabled) {
                    break
                }
            }
        } catch (e: Exception) {
            log.error("Job execution error", e)
        } finally {
            job.running = false
        }
    }

    @Data
    private class JobInstance(val sourceId: String, val job: Job) {
        @Transient
        var enabled = true
        var future: ScheduledFuture<*>? = null
        @Transient
        var running = false
    }
}
