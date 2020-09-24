package ru.citeck.ecos.records3.spring.utils.registrar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.source.dao.RecordsDao;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class RecordsDaoRegistrar {

    private final RecordsService recordsService;
    private List<RecordsDao> sources;

    @Autowired
    public RecordsDaoRegistrar(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @PostConstruct
    public void register() {
        log.info("========================== RecordsDaoRegistrar ==========================");
        if (sources != null) {
            sources.forEach(this::register);
        }
        log.info("========================= /RecordsDaoRegistrar ==========================");
    }

    private void register(RecordsDao dao) {
        log.info("Register: \"" + dao.getId() + "\" with class " + dao.getClass().getName());
        recordsService.register(dao);
    }

    @Autowired(required = false)
    public void setSources(List<RecordsDao> sources) {
        this.sources = sources;
    }
}
