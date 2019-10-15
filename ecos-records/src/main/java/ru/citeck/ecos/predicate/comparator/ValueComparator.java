package ru.citeck.ecos.predicate.comparator;

public interface ValueComparator {

    boolean isEquals(Object value0, Object value1);

    boolean isContains(Object value, Object subValue);

    boolean isIn(Object value, Object inValue);

    boolean isGreaterThan(Object value, Object thanValue, boolean inclusive);

    boolean isLessThan(Object value, Object thanValue, boolean inclusive);

    boolean isLike(Object value, Object likeValue);

    boolean isEmpty(Object value);
}
