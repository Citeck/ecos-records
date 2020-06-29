package ru.citeck.ecos.records2.meta.attproc;

import org.jetbrains.annotations.NotNull;
import ru.citeck.ecos.commons.data.DataValue;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FormatAttProcessor implements AttProcessor {

    @NotNull
    @Override
    public Object process(@NotNull DataValue value, @NotNull List<DataValue> arguments) {

        if (value.isTextual() && value.asText().endsWith("Z")) {

            SimpleDateFormat formatter = new SimpleDateFormat(arguments.get(0).asText(), Locale.ENGLISH);

            if (arguments.size() > 1) {
                String timezone = arguments.get(1).asText();
                if ("DEFAULT".equals(timezone)) {
                    formatter.setTimeZone(TimeZone.getDefault());
                } else {
                    formatter.setTimeZone(TimeZone.getTimeZone(timezone));
                }
            } else {
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            }

            return formatter.format(Date.from(Instant.parse(value.asText())));

        } else if (value.isNumber() || value.isTextual()) {
            DecimalFormat format = new DecimalFormat(
                arguments.get(0).asText(),
                DecimalFormatSymbols.getInstance(Locale.ENGLISH)
            );
            return format.format(value.asDouble());
        }
        return value;
    }

    @Override
    public String getType() {
        return "fmt";
    }
}
