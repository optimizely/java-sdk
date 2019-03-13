package com.optimizely.ab.api;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.Attribute;

import java.util.List;

public interface ImpressionEvent extends Event {
    void setUserId(String userId);

    Experiment getExperiment();

    Variation getVariation();

    String getUserId();

    List<Attribute> getUserAttributes();
}
