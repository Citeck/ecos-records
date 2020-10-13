package ru.citeck.ecos.records3.record.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.citeck.ecos.commons.utils.MandatoryParam;
import ru.citeck.ecos.commons.utils.StringUtils;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;

import java.util.*;

@Getter
@AllArgsConstructor
public class RequestCtxData<T> {

    private final String requestId;
    private final List<String> requestTrace;
    private final Locale locale;
    private final Map<String, Object> ctxAtts;
    private final boolean computedAttsDisabled;
    private final MsgLevel msgLevel;

    public static <T> Builder<T> create() {
        return new Builder<>();
    }

    public Builder<T> modify() {
        return new Builder<>(this);
    }

    public static class Builder<T> {

        private String requestId;
        private List<String> requestTrace;
        private Locale locale = Locale.ENGLISH;

        private Map<String, Object> contextAttributes = Collections.emptyMap();

        private boolean compAttsDisabled;
        private MsgLevel msgLevel = MsgLevel.WARN;

        public Builder() {
        }

        public Builder(RequestCtxData<T> base) {
            this.requestId = base.getRequestId();
            this.requestTrace = base.getRequestTrace();
            this.locale = base.getLocale();
            this.contextAttributes = base.ctxAtts;
            this.compAttsDisabled = base.computedAttsDisabled;
            this.msgLevel = base.msgLevel;
        }

        public Builder<T> merge(RequestCtxData<T> other) {
            this.contextAttributes = new HashMap<>(this.contextAttributes);
            other.getCtxAtts().forEach((k, v) -> this.contextAttributes.put(k, v));
            this.compAttsDisabled = other.computedAttsDisabled;
            return this;
        }

        public Builder<T> setMsgLevel(MsgLevel msgLevel) {
            this.msgLevel = msgLevel;
            return this;
        }

        public Builder<T> setCompAttsDisabled(boolean computedAttsDisabled) {
            this.compAttsDisabled = computedAttsDisabled;
            return this;
        }

        public Builder<T> setCtxAtts(Map<String, Object> contextAttributes) {
            this.contextAttributes = contextAttributes;
            return this;
        }

        public Builder<T> setRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder<T> setRequestTrace(List<String> requestTrace) {
            this.requestTrace = requestTrace;
            return this;
        }

        public Builder<T> setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public RequestCtxData<T> build() {

            MandatoryParam.check("locale", locale);

            if (StringUtils.isBlank(requestId)) {
                requestId = UUID.randomUUID().toString();
            }
            if (requestTrace == null) {
                requestTrace = Collections.emptyList();
            }
            return new RequestCtxData<>(
                requestId,
                Collections.unmodifiableList(requestTrace),
                locale,
                Collections.unmodifiableMap(contextAttributes),
                compAttsDisabled,
                msgLevel
            );
        }
    }
}
