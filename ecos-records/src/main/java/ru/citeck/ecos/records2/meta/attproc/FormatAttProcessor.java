package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.utils.StringUtils;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FormatAttProcessor implements AttProcessor {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC");

    private static final int LOCALE_ARG_IDX = 1;
    private static final int TIMEZONE_ARG_IDX = 2;

    @NotNull
    @Override
    public Object process(@NotNull DataValue value, @NotNull List<DataValue> arguments) {

        Locale locale = DEFAULT_LOCALE;
        if (arguments.size() > LOCALE_ARG_IDX) {
            String localeStr = arguments.get(LOCALE_ARG_IDX).asText();
            if (StringUtils.isNotBlank(localeStr)) {
                if ("DEFAULT".equals(localeStr)) {
                    locale = Locale.getDefault();
                } else {
                    locale = new Locale(localeStr);
                }
            }
        }

        TimeZone timeZone = DEFAULT_TIMEZONE;
        if (arguments.size() > TIMEZONE_ARG_IDX) {
            String timeZoneStr = arguments.get(TIMEZONE_ARG_IDX).asText();
            if (StringUtils.isNotBlank(timeZoneStr)) {
                if ("DEFAULT".equals(timeZoneStr)) {
                    timeZone = TimeZone.getDefault();
                } else {
                    timeZone = TimeZone.getTimeZone(timeZoneStr);
                }
            }
        }

        if (value.isTextual() && value.asText().endsWith("Z")) {

            SimpleDateFormat formatter = new SimpleDateFormat(arguments.get(0).asText(), locale);
            formatter.setTimeZone(timeZone);
            return formatter.format(Date.from(Instant.parse(value.asText())));

        } else if (value.isNumber() || value.isTextual()) {

            double parsed = value.asDouble(Double.NaN);
            if (!Double.isNaN(parsed)) {

                DecimalFormat format = new DecimalFormat(
                    arguments.get(0).asText(),
                    DecimalFormatSymbols.getInstance(locale)
                );

                return format.format(parsed);
            }
        }
        return value;
    }

    @Override
    public String getType() {
        return "fmt";
    }
}
