package ru.citeck.ecos.records2.spring.utils.registrar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class RecordsDAORegistrar {

    private RecordsService recordsService;
    private List<RecordsDAO> sources;

    @Autowired
    public RecordsDAORegistrar(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @PostConstruct
    public void register() {
        log.info("========================== RecordsDAORegistrar ==========================");
        if (sources != null) {
            sources.forEach(this::register);
        }
        log.info("========================= /RecordsDAORegistrar ==========================");
    }

    private void register(RecordsDAO dao) {
        log.info("Register: \"" + dao.getId() + "\" with class " + dao.getClass().getName());
        recordsService.register(dao);
    }

    @Autowired(required = false)
    public void setSources(List<RecordsDAO> sources) {
        this.sources = sources;
    }
}
