package ru.citeck.ecos.records2.graphql.meta.value;

import java.util.Collection;

public interface HasCollectionView<T> {

    Collection<T> getCollectionView();
}
