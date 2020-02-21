package ru.citeck.ecos.records2.source.dao.local;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MetaRecordsDAO extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object> {

    private static final Instant startedTime = Instant.now();

    public static final String ID = "meta";

    public MetaRecordsDAO() {
        setId(ID);
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {

        return records.stream().map(r -> {
            if (r.getId().isEmpty()) {
                return new MetaRoot();
            } else {
                return EmptyValue.INSTANCE;
            }
        }).collect(Collectors.toList());
    }

    public class MetaRoot implements MetaValue {

        @Override
        public Object getAttribute(String name, MetaField field) {
            switch (name) {
                case "rec":
                    return new Records();
                case "time":
                    return new Time();
            }
            return null;
        }
    }

    public class Time implements MetaValue {

        @Override
        public Object getAttribute(String name, MetaField field) {

            switch (name) {
                case "started":
                    return startedTime;
                case "uptime":
                    return getFormattedUptime();
                case "uptimeMs":
                    return getUptimeMs();
            }
            return null;
        }

        @Override
        public String getString() {
            return getFormattedUptime();
        }

        private long getUptimeMs() {
            return ChronoUnit.MILLIS.between(startedTime, Instant.now());
        }

        private String getFormattedUptime() {

            long uptime = getUptimeMs();

            long hours = TimeUnit.MILLISECONDS.toHours(uptime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime);

            String result = "";
            if (hours > 0) {
                result += hours + "h ";
            }
            if (hours > 0 || minutes > 0) {
                result += minutes + "m ";
            }
            if (hours > 0 || minutes > 0 || seconds > 0) {
                result += seconds + "s";
            }
            if (result.isEmpty()) {
                result = uptime + " ms";
            }

            return result;
        }
    }

    public class Records implements MetaValue {

        @Override
        public Object getAttribute(String name, MetaField field) {
            return RecordRef.valueOf(name);
        }
    }
}
