package com.optimizely.ab.cmab;

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.cmab.service.CmabDecision;
import com.optimizely.ab.cmab.service.CmabService;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;

import java.util.List;

public class DefaultCmabService implements CmabService {
    @Override
    public CmabDecision getDecision(ProjectConfig projectConfig, OptimizelyUserContext userContext, String ruleId, List<OptimizelyDecideOption> options) {
        return null;
    }
}
