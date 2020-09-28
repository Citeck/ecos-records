package ru.citeck.ecos.records3.source.dao.local.meta;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.records3.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.operation.meta.value.impl.EmptyValue;
import ru.citeck.ecos.records3.record.operation.meta.value.AttValue;
import ru.citeck.ecos.records3.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records3.source.dao.local.v2.LocalRecordsMetaDao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MetaRecordsDao extends LocalRecordsDao implements LocalRecordsMetaDao {

    private static final Instant STARTED_TIME = Instant.now();

    public static final String ID = "meta";

    private final MetaRecordsDaoAttsProvider metaRecordsDaoAttsProvider;

    public MetaRecordsDao(RecordsServiceFactory serviceFactory) {
        setId(ID);
        metaRecordsDaoAttsProvider = serviceFactory.getMetaRecordsDaoAttsProvider();
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> records) {

        return records.stream().map(r -> {
            if (r.getId().isEmpty()) {
                return new MetaRoot(metaRecordsDaoAttsProvider);
            } else {
                return EmptyValue.INSTANCE;
            }
        }).collect(Collectors.toList());
    }

    public static class MetaRoot implements AttValue {

        MetaRecordsDaoAttsProvider attsProvider;

        MetaRoot(MetaRecordsDaoAttsProvider attsProvider) {
            this.attsProvider = attsProvider;
        }

        @Override
        public Object getAtt(@NotNull String name) {
            switch (name) {
                case "rec":
                    return new Records();
                case "time":
                    return new Time();
                case "attributes":
                    return attsProvider.getAttributes();
                default:
                    return null;
            }
        }
    }

    public static class Time implements AttValue {

        @Override
        public Object getAtt(@NotNull String name) {

            switch (name) {
                case "started":
                    return STARTED_TIME;
                case "uptime":
                    return getFormattedUptime();
                case "uptimeMs":
                    return getUptimeMs();
                default:
                    return null;
            }
        }

        @Override
        public String getString() {
            return getFormattedUptime();
        }

        private long getUptimeMs() {
            return ChronoUnit.MILLIS.between(STARTED_TIME, Instant.now());
        }

        private String getFormattedUptime() {

            long uptimeMs = getUptimeMs();
            long uptimeLeft = uptimeMs;

            long hours = TimeUnit.MILLISECONDS.toHours(uptimeLeft);
            uptimeLeft -= TimeUnit.HOURS.toMillis(hours);

            long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeLeft);
            uptimeLeft -= TimeUnit.MINUTES.toMillis(minutes);

            long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeLeft);

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
                result = uptimeMs + " ms";
            }

            return result;
        }
    }

    public static class Records implements AttValue {

        @Override
        public Object getAtt(@NotNull String name) {
            return RecordRef.valueOf(name);
        }
    }
}
