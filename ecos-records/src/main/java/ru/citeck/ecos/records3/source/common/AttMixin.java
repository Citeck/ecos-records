package ru.citeck.ecos.records3.source.common;

public interface AttMixin {

    Object getAtt(String path, AttValueCtx value) throws Exception;

    boolean isProvides(String path);
}
