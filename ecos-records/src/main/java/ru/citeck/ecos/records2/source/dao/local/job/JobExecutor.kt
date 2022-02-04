package ru.citeck.ecos.records2.source.dao.local.job;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.request.RequestContext;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Predicate;

@Slf4j
@SuppressFBWarnings(value = {
    // For @Getter(lazy = true)
    "JLM_JSR166_UTILCONCURRENT_MONITORENTER",
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
})
public class JobExecutor {

    private static final String SYSTEM_JOBS_SOURCE_ID = "SYSTEM";

    private final List<JobInstance> jobs = new CopyOnWriteArrayList<>();
    private final RecordsServiceFactory serviceFactory;
    private ScheduledExecutorService executor;

    private boolean initialized = false;

    public JobExecutor(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public synchronized void init(ScheduledExecutorService executor) {

        if (initialized) {
            return;
        }

        if (executor == null) {
            ScheduledExecutorService newScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered");
                jobs.forEach(j -> j.setEnabled(false));
                removeJobs(j -> true);
                newScheduledExecutor.shutdown();
                log.info("Shutdown hook completed");
            }));
            executor = newScheduledExecutor;
        }
        this.executor = executor;

        for (JobInstance job : jobs) {
            scheduleJob(job);
        }

        initialized = true;
    }

    private void scheduleJob(JobInstance instance) {
        ScheduledFuture<?> future;
        if (instance.job instanceof PeriodicJob) {
            future = executor.scheduleAtFixedRate(
                () -> this.execute(instance),
                instance.job.getInitDelay(),
                ((PeriodicJob) instance.job).getPeriod(),
                TimeUnit.MILLISECONDS
            );
        } else {
            future = executor.schedule(
                () -> this.execute(instance),
                instance.job.getInitDelay(),
                TimeUnit.MILLISECONDS
            );
        }
        instance.setFuture(future);
    }

    public synchronized void addJobs(String sourceId, List<Job> jobs) {
        jobs.forEach(j -> addJob(sourceId, j));
    }

    public synchronized void addSystemJob(Job job) {
        addJob(SYSTEM_JOBS_SOURCE_ID, job);
    }

    public synchronized void addJob(String sourceId, Job job) {

        JobInstance instance = new JobInstance(sourceId, job);
        jobs.add(instance);

        if (initialized) {
            scheduleJob(instance);
        }
    }

    public void removeJobs(String sourceId) {
        removeJobs(instance -> Objects.equals(instance.sourceId, sourceId));
    }

    private synchronized void removeJobs(Predicate<JobInstance> filter) {

        jobs.removeIf(instance -> {
            if (!filter.test(instance)) {
                return false;
            }
            instance.enabled = false;
            if (!initialized || instance.future == null) {
                return true;
            }
            try {
                if (instance.isRunning) {

                    long waitUntilRunning = System.currentTimeMillis() + 5_000;

                    log.info("Job from sourceId '" + instance.sourceId + "' is running. Try to wait until "
                        + Instant.ofEpochMilli(waitUntilRunning));

                    while (instance.isRunning && System.currentTimeMillis() < waitUntilRunning) {
                        Thread.sleep(100);
                    }

                    if (instance.isRunning) {
                        log.warn("Job is still running and will be cancelled");
                    } else {
                        log.info("Job is not running");
                    }
                }
                instance.future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Exception while job cancelling. SourceId: " + instance.sourceId, e);
            }
            return true;
        });
    }

    private void execute(JobInstance job) {

        if (!job.enabled) {
            return;
        }

        job.setRunning(true);

        try {

            int count = 0;

            while (count++ < 20) {

                if (!RequestContext.doWithCtx(serviceFactory,
                    ctx -> AuthContext.runAsSystem(job.job::execute))) {

                    break;
                }
                if (!job.enabled) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Job execution error", e);
        } finally {
            job.setRunning(false);
        }
    }

    @Data
    private static class JobInstance {

        private final String sourceId;
        private final Job job;

        private boolean enabled = true;
        private ScheduledFuture<?> future;
        private boolean isRunning;

        public JobInstance(String sourceId, Job job) {
            this.sourceId = sourceId;
            this.job = job;
        }
    }
}
