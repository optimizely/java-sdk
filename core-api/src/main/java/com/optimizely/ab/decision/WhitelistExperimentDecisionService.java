package com.optimizely.ab.decision;

import com.optimizely.ab.bucketing.Decision;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;

import java.util.Map;

public class WhitelistExperimentDecisionService implements DecisionService<Experiment, Decision> {
    @Override
    public Decision getDecision(Experiment entity, String userId, Map<String, ?> attributes, ProjectConfig projectConfig) {
        return null;
    }
}
