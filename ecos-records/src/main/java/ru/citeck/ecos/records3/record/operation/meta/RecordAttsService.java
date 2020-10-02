package ru.citeck.ecos.records3.record.operation.meta;

import ru.citeck.ecos.records3.RecordAtts;
import ru.citeck.ecos.records3.source.common.AttMixin;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RecordAttsService {

    RecordAtts getAtts(Object value, Map<String, String> attributes);

    RecordAtts getAtts(Object value, Map<String, String> attributes, boolean rawAtts);

    RecordAtts getAtts(Object value, Collection<String> attributes);

    RecordAtts getAtts(Object value, Collection<String> attributes, boolean rawAtts);

    <T> T getAtts(Object value, Class<T> attsDto);

    <T> List<T> getAtts(List<?> values, Class<T> attsDto);

    List<RecordAtts> getAtts(List<?> values, Collection<String> attributes);

    List<RecordAtts> getAtts(List<?> values, Collection<String> attributes, boolean rawAtts);

    List<RecordAtts> getAtts(List<?> values, Map<String, String> attributes);

    List<RecordAtts> getAtts(List<?> values, Map<String, String> attributes, boolean rawAtts);

    List<RecordAtts> getAtts(List<?> values, Map<String, String> attributes, boolean rawAtts, List<AttMixin> mixins);
}
