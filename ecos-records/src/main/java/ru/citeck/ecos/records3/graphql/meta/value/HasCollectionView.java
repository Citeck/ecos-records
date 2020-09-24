package ru.citeck.ecos.records3.graphql.meta.value;

import java.util.Collection;

public interface HasCollectionView<T> {

    Collection<T> getCollectionView();
}
