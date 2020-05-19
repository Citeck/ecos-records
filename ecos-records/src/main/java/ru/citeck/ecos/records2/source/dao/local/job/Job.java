package ru.citeck.ecos.records2.source.dao.local.job;

public interface Job {

    long getInitDelay();

    boolean execute();
}
