package ru.citeck.ecos.records3.record.operation.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

import java.util.List;

public interface RecordsDeleteDao {

    List<RecDelStatus> delete(@NotNull List<RecordRef> records);
}
