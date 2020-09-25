package ru.citeck.ecos.records3.record.operation.meta;

import ru.citeck.ecos.records3.RecordMeta;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RecordAttsService {

    RecordMeta getAtts(Object value, Map<String, String> attributes);

    RecordMeta getAtts(Object value, Map<String, String> attributes, boolean rawAtts);

    RecordMeta getAtts(Object value, Collection<String> attributes);

    RecordMeta getAtts(Object value, Collection<String> attributes, boolean rawAtts);

    <T> T getAtts(Object value, Class<T> attsDto);

    List<RecordMeta> getAtts(List<Object> values, Map<String, String> attributes);

    List<RecordMeta> getAtts(List<Object> values, Map<String, String> attributes, boolean rawAtts);

    List<RecordMeta> getAtts(List<Object> values, Collection<String> attributes);

    List<RecordMeta> getAtts(List<Object> values, Collection<String> attributes, boolean rawAtts);

    <T> List<T> getAtts(List<Object> values, Class<T> attsDto);
}
