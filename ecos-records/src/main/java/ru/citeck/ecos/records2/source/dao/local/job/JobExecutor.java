package ru.citeck.ecos.records2.source.dao.local.job;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressFBWarnings(value = {
    // For @Getter(lazy = true)
    "JLM_JSR166_UTILCONCURRENT_MONITORENTER",
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"
})
public class JobExecutor {

    @Getter(lazy = true)
    private final ScheduledExecutorService executorService = createExecutor();

    private final List<Job> jobs = new CopyOnWriteArrayList<>();

    public void init() {
        for (Job job : jobs) {
            ScheduledExecutorService executor = getExecutorService();
            if (job instanceof PeriodicJob) {
                executor.scheduleAtFixedRate(
                    () -> this.execute(job),
                    job.getInitDelay(),
                    ((PeriodicJob) job).getPeriod(),
                    TimeUnit.MILLISECONDS
                );
            } else {
                executor.schedule(
                    () -> this.execute(job),
                    job.getInitDelay(),
                    TimeUnit.MILLISECONDS
                );
            }
        }
    }

    public void addJob(Job job) {
        jobs.add(job);
    }

    private void execute(Job job) {
        try {
            int count = 0;
            while (count++ < 20) {
                if (!job.execute()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Job execution error", e);
        }
    }

    private ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
