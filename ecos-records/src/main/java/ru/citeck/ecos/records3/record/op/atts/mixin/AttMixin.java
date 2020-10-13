package ru.citeck.ecos.records3.record.op.atts.mixin;

import ru.citeck.ecos.records3.record.op.atts.schema.resolver.AttValueCtx;

import java.util.Collection;

public interface AttMixin {

    Object getAtt(String path, AttValueCtx value) throws Exception;

    Collection<String> getProvidedAtts();
}
