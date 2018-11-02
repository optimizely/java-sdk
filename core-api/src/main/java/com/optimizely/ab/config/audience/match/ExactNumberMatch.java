package com.optimizely.ab.config.audience.match;

import javax.annotation.Nullable;

// Because json number is a double in most java json parsers.  at this
// point we allow comparision of Integer and Double.  The instance class is Double and
// Integer which would fail in our normal exact match.  So, we are special casing for now.  We have already filtered
// out other Number types.
public class ExactNumberMatch extends AttributeMatch<Number> {
    Number value;

    protected ExactNumberMatch(Number value) {
        this.value = value;
    }

    public @Nullable
    Boolean eval(Object attributeValue) {
        try {
            return value.doubleValue() == convert(attributeValue).doubleValue();
        } catch (Exception e) {
            MatchType.logger.error("Exact number match failed ", e);
        }

        return null;
    }
}
