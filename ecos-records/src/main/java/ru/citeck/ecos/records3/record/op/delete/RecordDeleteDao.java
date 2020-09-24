package ru.citeck.ecos.records3.record.op.delete;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;

public interface RecordDeleteDao {

    RecDelStatus delete(@NotNull RecordRef record);
}
