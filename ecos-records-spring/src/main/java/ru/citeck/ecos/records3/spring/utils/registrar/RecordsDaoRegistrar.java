package ru.citeck.ecos.records3.spring.utils.registrar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.dao.RecordsDao;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class RecordsDaoRegistrar {

    private final RecordsService recordsService;
    private final ru.citeck.ecos.records2.RecordsService recordsServiceV0;

    private List<RecordsDao> sources;
    private List<ru.citeck.ecos.records2.source.dao.RecordsDao> sourcesV0;

    @Autowired
    public RecordsDaoRegistrar(RecordsService recordsService, ru.citeck.ecos.records2.RecordsService recordsServiceV0) {
        this.recordsService = recordsService;
        this.recordsServiceV0 = recordsServiceV0;
    }

    @PostConstruct
    public void register() {
        log.info("========================== RecordsDaoRegistrar ==========================");
        if (sources != null) {
            sources.forEach(this::register);
        }
        if (sourcesV0 != null) {
            sourcesV0.forEach(this::register);
        }
        log.info("========================= /RecordsDaoRegistrar ==========================");
    }

    private void register(RecordsDao dao) {
        log.info("Register: \"" + dao.getId() + "\" with class " + dao.getClass().getName());
        recordsService.register(dao);
    }

    private void register(ru.citeck.ecos.records2.source.dao.RecordsDao dao) {
        log.info("Register: \"" + dao.getId() + "\" with class " + dao.getClass().getName());
        recordsServiceV0.register(dao);
    }

    @Autowired(required = false)
    public void setSources(List<RecordsDao> sources) {
        this.sources = sources;
    }

    @Autowired(required = false)
    public void setSourcesV0(List<ru.citeck.ecos.records2.source.dao.RecordsDao> sourcesV0) {
        this.sourcesV0 = sourcesV0;
    }
}
