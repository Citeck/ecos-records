package ru.citeck.ecos.records2.source.dao.local.job

import lombok.Data
import mu.KotlinLogging
import ru.citeck.ecos.commons.task.schedule.Schedules
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.task.scheduler.EcosScheduledTask
import ru.citeck.ecos.webapp.api.task.scheduler.EcosTaskSchedulerApi
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate

class JobExecutor(private val serviceFactory: RecordsServiceFactory) {

    companion object {
        private const val SYSTEM_JOBS_SOURCE_ID = "SYSTEM"
        private const val SCHEDULER_ID = "records"

        private val log = KotlinLogging.logger {}

        private val systemJobsCounter = AtomicInteger()
    }

    private val jobs: MutableList<JobInstance> = CopyOnWriteArrayList()
    private val scheduler: EcosTaskSchedulerApi? = serviceFactory.getEcosWebAppApi()
        ?.getTasksApi()
        ?.getScheduler(SCHEDULER_ID)

    @Volatile
    private var initialized = false

    init {
        serviceFactory.getEcosWebAppApi()?.doWhenAppReady { init() }
    }

    fun init() {
        if (initialized) {
            return
        }
        if (scheduler != null) {
            log.info { "Initialize JobExecutor" }
            synchronized(jobs) {
                for (job in jobs) {
                    scheduleJob(job)
                }
            }
        } else {
            log.info { "JobExecutor is disabled because scheduler is null" }
        }
        initialized = true
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    private fun scheduleJob(instance: JobInstance) {
        val scheduler = this.scheduler ?: return
        instance.task = if (instance.job is PeriodicJob) {
            scheduler.schedule(
                instance.getId(),
                Schedules.fixedDelay(
                    Duration.ofMillis(instance.job.getInitDelay()),
                    Duration.ofMillis(instance.job.period)
                )
            ) { execute(instance) }
        } else {
            scheduler.schedule(
                instance.getId(),
                Schedules.once(Duration.ofMillis(instance.job.initDelay))
            ) { execute(instance) }
        }
    }

    @Synchronized
    fun addJobs(sourceId: String, jobs: List<Job>) {
        jobs.forEachIndexed { idx, job -> addJob(idx, sourceId, job) }
    }

    @Synchronized
    fun addSystemJob(job: Job) {
        addJob(systemJobsCounter.getAndIncrement(), SYSTEM_JOBS_SOURCE_ID, job)
    }

    @Synchronized
    fun addJob(idx: Int, sourceId: String, job: Job) {
        val instance = JobInstance(idx, sourceId, job)
        synchronized(jobs) {
            jobs.add(instance)
            if (initialized) {
                scheduleJob(instance)
            }
        }
    }

    fun removeJobs(sourceId: String?) {
        removeJobs { it.sourceId == sourceId }
    }

    @Synchronized
    private fun removeJobs(filter: Predicate<JobInstance>) {
        val jobsToCancel = mutableListOf<JobInstance>()
        synchronized(jobs) {
            jobs.removeIf {
                if (filter.test(it)) {
                    jobsToCancel.add(it)
                    true
                } else {
                    false
                }
            }
        }
        jobsToCancel.map {
            it to it.task?.cancel(Duration.ofSeconds(10))
        }.forEach {
            try {
                it.second?.get(Duration.ofSeconds(20))
            } catch (e: Exception) {
                log.warn("Exception while job cancelling. SourceId: " + it.first.sourceId, e)
            }
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
    private class JobInstance(val idx: Int, val sourceId: String, val job: Job) {
        @Transient
        var enabled = true
        var task: EcosScheduledTask? = null
        @Transient
        var running = false

        fun getId(): String = "records_${sourceId}_$idx"
    }
}
