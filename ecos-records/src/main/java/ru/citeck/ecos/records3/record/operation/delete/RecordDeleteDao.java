package ru.citeck.ecos.records3.record.operation.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

public interface RecordDeleteDao {

    RecDelStatus delete(@NotNull RecordRef record);
}
