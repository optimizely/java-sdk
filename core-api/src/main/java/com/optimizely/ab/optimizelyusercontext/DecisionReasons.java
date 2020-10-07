package com.optimizely.ab.optimizelyusercontext;

import java.util.ArrayList;
import java.util.List;

public class DecisionReasons {

    List<String> errors;
    List<String> logs;

    public DecisionReasons() {
        this.errors = new ArrayList<String>();
        this.logs = new ArrayList<String>();
    }

    public void addError(String message) {
        errors.add(message);
    }

    public void addInfo(String message) {
        logs.add(message);
    }


    public List<String> toReport(List<OptimizelyDecideOption> options) {
        List<String> reasons = new ArrayList<>(errors);
        if(options.contains(OptimizelyDecideOption.INCLUDE_REASONS)) {
            reasons.addAll(logs);
        }
        return reasons;
    }

}
