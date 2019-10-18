package ru.citeck.ecos.records2.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;

import java.util.List;

@Slf4j
@Component
public class RecordsDAORegistrar {

    private RecordsService recordsService;

    @Autowired
    public RecordsDAORegistrar(RecordsService recordsService, List<RecordsDAO> sources) {
        log.info("========================== RecordsDAORegistrar ==========================");
        this.recordsService = recordsService;
        sources.forEach(this::register);
        log.info("========================= /RecordsDAORegistrar ==========================");
    }

    private void register(RecordsDAO dao) {
        log.info("Register: \"" + dao.getId() + "\" with class " + dao.getClass().getName());
        recordsService.register(dao);
    }
}
