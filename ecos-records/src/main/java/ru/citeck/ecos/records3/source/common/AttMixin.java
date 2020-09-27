package ru.citeck.ecos.records3.source.common;

import java.util.Collection;

public interface AttMixin {

    Object getAtt(String name, AttValueCtx value) throws Exception;

    Collection<String> getAtts();
}
