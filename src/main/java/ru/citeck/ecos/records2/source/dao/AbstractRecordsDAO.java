package ru.citeck.ecos.records2.source.dao;

public abstract class AbstractRecordsDAO implements RecordsDAO {

    private String id;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
