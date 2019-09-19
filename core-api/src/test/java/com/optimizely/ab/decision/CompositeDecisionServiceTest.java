package com.optimizely.ab.decision;

import com.optimizely.ab.bucketing.Decision;
import com.optimizely.ab.config.Experiment;
import org.junit.Test;

import java.util.Collections;

public class CompositeDecisionServiceTest {

    @Test
    public void testBuilder() {
        CompositeDecisionService.Builder<Experiment, Decision> builder = CompositeDecisionService.builder();

        builder.addDecisionService(new UserProfileDecisionService());
        builder.addDecisionService(new WhitelistExperimentDecisionService());
        builder.addDecisionService((x,y,z,w) -> new Decision("FORCED DECISION"));
        builder.addDecisionService(new BucketedExperimentDecisionService());

        DecisionService<Experiment, Decision> decisionService = builder.build();

        Experiment experiment = new Experiment("id", "key", "layer");
        Decision decision = decisionService.getDecision(experiment, "me", Collections.emptyMap(), null);

        if (decision == null) {
            System.out.println("Decision NOT made :(");
        } else {
            System.out.println("Decision made: " + decision.variationId);
        }

    }
}
