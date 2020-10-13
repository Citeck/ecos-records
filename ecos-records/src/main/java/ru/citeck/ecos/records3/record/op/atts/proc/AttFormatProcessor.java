package ru.citeck.ecos.records3.record.op.atts.proc;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.utils.StringUtils;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AttFormatProcessor extends AbstractAttProcessor<AttFormatProcessor.Args> {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC");

    private static final int LOCALE_ARG_IDX = 1;
    private static final int TIMEZONE_ARG_IDX = 2;

    @Override
    protected Object processOne(@NotNull ObjectData meta, @NotNull DataValue value, @NotNull Args args) {

        if (value.isTextual() && value.asText().endsWith("Z")) {

            SimpleDateFormat formatter = new SimpleDateFormat(args.getFormat(), args.getLocale());
            formatter.setTimeZone(args.getTimeZone());
            return formatter.format(Date.from(Instant.parse(value.asText())));

        } else if (value.isNumber() || value.isTextual()) {

            double parsed = value.asDouble(Double.NaN);
            if (!Double.isNaN(parsed)) {

                DecimalFormat format = new DecimalFormat(
                    args.getFormat(),
                    DecimalFormatSymbols.getInstance(args.getLocale())
                );

                return format.format(parsed);
            }
        }
        return value;
    }

    @Override
    protected Args parseArgs(@NotNull List<DataValue> argValues) {

        Locale locale = DEFAULT_LOCALE;
        if (argValues.size() > LOCALE_ARG_IDX) {
            String localeStr = argValues.get(LOCALE_ARG_IDX).asText();
            if (StringUtils.isNotBlank(localeStr)) {
                if ("DEFAULT".equals(localeStr)) {
                    locale = Locale.getDefault();
                } else {
                    locale = new Locale(localeStr);
                }
            }
        }

        TimeZone timeZone = DEFAULT_TIMEZONE;
        if (argValues.size() > TIMEZONE_ARG_IDX) {
            String timeZoneStr = argValues.get(TIMEZONE_ARG_IDX).asText();
            if (StringUtils.isNotBlank(timeZoneStr)) {
                if ("DEFAULT".equals(timeZoneStr)) {
                    timeZone = TimeZone.getDefault();
                } else {
                    timeZone = TimeZone.getTimeZone(timeZoneStr);
                }
            }
        }

        return new Args(argValues.get(0).asText(), locale, timeZone);
    }

    @NotNull
    @Override
    public String getType() {
        return "fmt";
    }

    @Data
    @RequiredArgsConstructor
    public static class Args {
        private final String format;
        private final Locale locale;
        private final TimeZone timeZone;
    }
}
