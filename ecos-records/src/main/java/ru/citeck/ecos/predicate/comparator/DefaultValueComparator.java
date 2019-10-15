package ru.citeck.ecos.predicate.comparator;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;

public class DefaultValueComparator implements ValueComparator {

    private static final double DOUBLE_THRESHOLD = 0.00000001;

    @Override
    public boolean isEquals(Object value0, Object value1) {

        if (value0 == value1) {
            return true;
        }
        if (value0 == null || value1 == null) {
            return false;
        }
        if (value0.getClass().equals(value1.getClass())) {
            return value0.equals(value1);
        }
        if (value0 instanceof Number && value1 instanceof Number) {
            double v0 = ((Number) value0).doubleValue();
            double v1 = ((Number) value1).doubleValue();
            return Math.abs(v0 - v1) < DOUBLE_THRESHOLD;
        }
        if (value0 instanceof String || value1 instanceof String) {
            return String.valueOf(value0).equals(String.valueOf(value1));
        }

        return false;
    }

    @Override
    public boolean isContains(Object value, Object subValue) {

        if (value == null || subValue == null) {
            return false;
        }

        if (value instanceof Collection) {
            return ((Collection) value).contains(subValue);
        }
        if (value instanceof String || subValue instanceof String) {
            String v0 = String.valueOf(value).toLowerCase();
            String v1 = String.valueOf(subValue).toLowerCase();
            return v0.contains(v1);
        }

        return false;
    }

    @Override
    public boolean isIn(Object value, Object inValue) {

        if (value == null || inValue == null) {
            return false;
        }

        if (inValue instanceof Collection) {
            for (Object collectionValue : (Collection) inValue) {
                if (isEquals(collectionValue, value)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isGreaterThan(Object value, Object thanValue, boolean inclusive) {
        return compareGL(value, thanValue, true, inclusive);
    }

    @Override
    public boolean isLessThan(Object value, Object thanValue, boolean inclusive) {
        return compareGL(value, thanValue, false, inclusive);
    }

    @Override
    public boolean isLike(Object value, Object likeValue) {

        if (value == null || likeValue == null) {
            return false;
        }

        String valueStr = String.valueOf(value).toLowerCase();
        String likeStr = String.valueOf(likeValue).toLowerCase();

        likeStr = likeStr.replace("%", ".*");
        likeStr = likeStr.replace("_", ".");

        return valueStr.matches(likeStr);
    }

    @Override
    public boolean isEmpty(Object value) {

        if (value == null) {
            return true;
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            return true;
        }

        return value instanceof Collection && ((Collection) value).isEmpty();
    }

    private boolean compareGL(Object value0,
                              Object value1,
                              boolean isGreater,
                              boolean inclusive) {

        if (value0 == null || value1 == null) {
            return false;
        }

        CompareResult result = compareDouble(value0, value1, isGreater, inclusive);
        if (result == CompareResult.UNKNOWN) {
            result = compareDate(value0, value1, isGreater, inclusive);
        }

        return result == CompareResult.TRUE;
    }

    private CompareResult compareDate(Object value0,
                                Object value1,
                                boolean isGreater,
                                boolean inclusive) {

        Date dateValue0 = parseDate(value0);
        if (dateValue0 == null) {
            return CompareResult.UNKNOWN;
        }
        Date dateValue1 = parseDate(value1);
        if (dateValue1 == null) {
            return CompareResult.UNKNOWN;
        }

        if (isGreater ? dateValue0.after(dateValue1) : dateValue0.before(dateValue1)) {
            return CompareResult.TRUE;
        } else if (inclusive) {
            if (dateValue0.getTime() == dateValue1.getTime()) {
                return CompareResult.TRUE;
            } else {
                return CompareResult.FALSE;
            }
        }

        return CompareResult.FALSE;
    }

    private Date parseDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof String) {
            try {
                value = OffsetDateTime.parse((String) value);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        if (value instanceof OffsetDateTime) {
            value = ((OffsetDateTime) value).toInstant();
        }
        if (value instanceof Instant) {
            value = Date.from((Instant) value);
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return null;
    }

    private CompareResult compareDouble(Object value0,
                                  Object value1,
                                  boolean isGreater,
                                  boolean inclusive) {

        Double doubleValue0 = parseDouble(value0);
        if (doubleValue0 == null) {
            return CompareResult.UNKNOWN;
        }
        Double doubleValue1 = parseDouble(value1);
        if (doubleValue1 == null) {
            return CompareResult.UNKNOWN;
        }

        if (isGreater ? doubleValue0 > doubleValue1 : doubleValue0 < doubleValue1) {
            return CompareResult.TRUE;
        } else if (inclusive) {
            if (Math.abs(doubleValue0 - doubleValue1) < DOUBLE_THRESHOLD) {
                return CompareResult.TRUE;
            } else {
                return CompareResult.FALSE;
            }
        }
        return CompareResult.FALSE;
    }

    private Double parseDouble(Object value) {

        double doubleValue0;
        if (value instanceof Number) {
            doubleValue0 = ((Number) value).doubleValue();
        } else {
            try {
                doubleValue0 = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return doubleValue0;
    }

    private enum CompareResult {
        TRUE, FALSE, UNKNOWN
    }
}
