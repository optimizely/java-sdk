package com.optimizely.ab.config.audience.match;

import javax.annotation.Nullable;
import java.util.List;

public class QualifiedMatch implements Match {
    @Nullable
    public Boolean eval(Object conditionValue, Object attributeValue) throws UnexpectedValueTypeException {
        if (!(conditionValue instanceof String)) {
            throw new UnexpectedValueTypeException();
        }

        if (!(attributeValue instanceof List)) {
            return null;
        }

        try {
            return ((List<String>)attributeValue).contains(conditionValue.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
