package ru.citeck.ecos.records2.source.dao.local.job;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface JobsProvider {

    @NotNull
    List<Job> getJobs();
}
