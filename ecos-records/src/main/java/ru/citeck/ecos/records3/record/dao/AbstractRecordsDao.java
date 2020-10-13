package ru.citeck.ecos.records3.record.dao;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records3.record.op.atts.mixin.AttMixin;
import ru.citeck.ecos.records3.record.op.atts.mixin.AttMixinsHolder;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public abstract class AbstractRecordsDao implements RecordsDao,
    AttMixinsHolder,
    ServiceFactoryAware {

    private String id;

    @Getter
    private final List<AttMixin> mixins = new CopyOnWriteArrayList<>();

    protected RecordsServiceFactory serviceFactory;
    protected PredicateService predicateService;
    protected RecordsService recordsService;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addAttributesMixin(AttMixin mixin) {
        mixins.add(mixin);
    }

    /**
     * Remove attributes mixin by reference equality.
     */
    public void removeAttributesMixin(AttMixin mixin) {
        mixins.removeIf(m -> m == mixin);
    }

    @Override
    public String toString() {
        return "[" + getId() + "](" + getClass().getName() + "@" + Integer.toHexString(hashCode()) + ")";
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        this.recordsService = serviceFactory.getRecordsServiceV1();
        this.predicateService = serviceFactory.getPredicateService();
    }
}
