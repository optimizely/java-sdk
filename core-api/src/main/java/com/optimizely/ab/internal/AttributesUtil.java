package com.optimizely.ab.internal;

public class AttributesUtil {

    /**
     * Validate that value is not infinite, NAN or greater than Math.pow(2, 53).
     *
     * @param value attribute value or condition value.
     * @return boolean value of is valid or not.
     */
    public static boolean isValidNumber(Object value) {
        if (value instanceof Integer) {
            return Math.abs((Integer) value) <= Math.pow(2, 53);
        } else if (value instanceof Double || value instanceof Float) {
            Double doubleValue = ((Number) value).doubleValue();
            return !(doubleValue.isNaN() || doubleValue.isInfinite() || Math.abs(doubleValue) > Math.pow(2, 53));
        } else if (value instanceof Long) {
            return Math.abs((Long) value) <= Math.pow(2, 53);
        }
        return false;
    }

}
