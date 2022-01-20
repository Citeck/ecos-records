package ru.citeck.ecos.records2.source.dao.local.job;

public interface Job {

    long getInitDelay();

    /**
     * @return true if job work is not completed
     */
    boolean execute();
}
