package ru.citeck.ecos.records3.source.dao.local.job;

public interface Job {

    long getInitDelay();

    boolean execute();
}
