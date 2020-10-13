package ru.citeck.ecos.records2.source.dao;

public abstract class AbstractRecordsDao implements RecordsDao {

    private String id;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
