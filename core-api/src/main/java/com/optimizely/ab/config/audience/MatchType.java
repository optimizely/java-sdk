package com.optimizely.ab.config.audience;

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

class ExactMatch<T> extends LeafMatch<T> {
    T value;
    protected ExactMatch(T value) {
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
            case "gt":
                if (value instanceof Number) {
                    return new MatchType(type, new GTMatch((Number) value));
                }
            case "lt":
                if (value instanceof Number) {
                    return new MatchType(type, new LTMatch((Number) value));
                }
            case "custom_dimension":
                if (value instanceof String) {
                    return new MatchType(type, new ExactMatch<String>((String) value));
                }
            default:
                return null;
        }
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
