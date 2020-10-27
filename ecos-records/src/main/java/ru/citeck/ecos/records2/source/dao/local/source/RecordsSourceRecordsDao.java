package ru.citeck.ecos.records2.source.dao.local.source;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.record.op.atts.dao.RecordsAttsDao;
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue;
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao;
import ru.citeck.ecos.records3.record.op.query.dto.RecordsQuery;
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.RecordsDaoInfo;

import java.util.List;
import java.util.stream.Collectors;

public class RecordsSourceRecordsDao extends AbstractRecordsDao
                                     implements RecordsQueryDao,
                                                RecordsAttsDao {

    public static final String ID = "source";

    @Override
    public List<?> getRecordsAtts(List<String> records) {

        return records.stream().map(rec -> {
            RecordsDaoInfo info = recordsService.getSourceInfo(rec);
            if (info == null) {
                return EmptyAttValue.INSTANCE;
            }
            return info;
        }).collect(Collectors.toList());
    }

    @Override
    public RecsQueryRes<?> queryRecords(@NotNull RecordsQuery query) {
        return new RecsQueryRes<>(recordsService.getSourcesInfo());
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }
}
