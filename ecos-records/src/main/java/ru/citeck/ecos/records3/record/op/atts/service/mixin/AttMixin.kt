package ru.citeck.ecos.records3.record.op.atts.service.mixin;

import ru.citeck.ecos.records3.record.op.atts.service.schema.resolver.AttValueCtx;

import java.util.Collection;

public interface AttMixin {

    Object getAtt(String path, AttValueCtx value) throws Exception;

    Collection<String> getProvidedAtts();
}
