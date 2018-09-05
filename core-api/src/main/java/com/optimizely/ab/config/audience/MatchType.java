package com.optimizely.ab.config.audience;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

interface LeafMatcher {
    public Boolean eval(Object otherValue);
}

abstract class LeafMatch<T> implements LeafMatcher {
    T convert(Object o) {
        try {
            T rv = (T)o;
            return rv;
        } catch(java.lang.ClassCastException e) {
            return null;
        }
    }
}

class NullMatch extends LeafMatch<Object> {
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    Object value;
    protected NullMatch() {
        this.value = null;
    }

    public @Nullable Boolean eval(Object otherValue) {
        return null;
    }
}

class ExactMatch<T> extends LeafMatch<T> {
    T value;
    protected ExactMatch(T value) {
        this.value = value;
    }

    public @Nullable Boolean eval(Object otherValue) {
        if (!value.getClass().isInstance(otherValue)) return null;
        return value.equals(convert(otherValue));
    }
}

/**
 * This is a temporary class.  It mimics the current behaviour for
 * custom dimension.  This will be dropped for ExactMatch and the unit tests need to be fixed.
 * @param <T>
 */
class CustomDimensionMatch<T> extends LeafMatch<T> {
    T value;
    protected CustomDimensionMatch(T value) {
        this.value = value;
    }

    public @Nullable Boolean eval(Object otherValue) {
        return value.equals(convert(otherValue));
    }
}

class GTMatch extends LeafMatch<Number> {
    Number value;
    protected GTMatch(Number value) {
        this.value = value;
    }

    public @Nullable Boolean eval(Object otherValue) {
       try {
           return ((Number) convert(otherValue)).doubleValue() > value.doubleValue();
       }
       catch (Exception e) {
           return null;
        }
    }
}

class LTMatch extends LeafMatch<Number> {
    Number value;
    protected LTMatch(Number value) {
        this.value = value;
    }

    public @Nullable Boolean eval(Object otherValue) {
        try {
            return ((Number) convert(otherValue)).doubleValue() < value.doubleValue();
        }
        catch (Exception e) {
            return null;
        }
    }
}

class ExistsMatch extends LeafMatch<Object> {
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    Object value;
    protected ExistsMatch(Object value) {
        this.value = value;
    }

    public @Nullable Boolean eval(Object otherValue) {
        try {
            return otherValue != null;
        }
        catch (Exception e) {
            return null;
        }
    }
}

class SubstringMatch extends LeafMatch<String> {
    String value;
    protected SubstringMatch(String value) {
        this.value = value;
    }

    public @Nullable Boolean eval(Object otherValue) {
        try {
            return value.contains(convert(otherValue));
        }
        catch (Exception e) {
            return null;
        }
    }
}

public class MatchType {

    private String type;
    private LeafMatcher matcher;

    public static MatchType getMatchType(String type, Object value) {
        switch (type) {
            case "exists":
                return new MatchType(type, new ExistsMatch(value));
            case "exact":
                if (value instanceof String) {
                    return new MatchType(type, new ExactMatch<String>((String) value));
                }
                else if (value instanceof Number) {
                    return new MatchType(type, new ExactMatch<Number>((Number) value));
                }
                else if (value instanceof Boolean) {
                    return new MatchType(type, new ExactMatch<Boolean>((Boolean) value));
                }
                break;
            case "substring":
                if (value instanceof String) {
                    return new MatchType(type, new SubstringMatch((String) value));
                }
                break;
            case "gt":
                if (value instanceof Number) {
                    return new MatchType(type, new GTMatch((Number) value));
                }
                break;
            case "lt":
                if (value instanceof Number) {
                    return new MatchType(type, new LTMatch((Number) value));
                }
                break;
            case "custom_dimension":
                if (value instanceof String) {
                    return new MatchType(type, new CustomDimensionMatch<String>((String) value));
                }
                break;
            default:
                return new MatchType(type, new NullMatch());
        }

        return new MatchType(type, new NullMatch());
    }


    private MatchType(String type, LeafMatcher matcher) {
        this.type = type;
        this.matcher = matcher;
    }

    public @Nonnull
    LeafMatcher getMatcher() {
        return matcher;
    }

    @Override
    public String toString() {
        return type;
    }
}