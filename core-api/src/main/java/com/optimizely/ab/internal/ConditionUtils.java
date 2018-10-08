package com.optimizely.ab.internal;

import com.google.gson.internal.LinkedTreeMap;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.AudienceHolderCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionUtils {
    static public Condition parseConditions(List<Object> rawObjectList) {
        List<Condition> conditions = new ArrayList<Condition>();
        String operand = (String)rawObjectList.get(0);

        for (int i = 1; i < rawObjectList.size(); i++) {
            Object obj = rawObjectList.get(i);
            if (obj instanceof List) {
                List<Object> objectList = (List<Object>)rawObjectList.get(i);
                conditions.add(parseConditions(objectList));
            }
            else if (obj instanceof String) {
                conditions.add(new AudienceHolderCondition((String)obj));
            }
            else if (obj instanceof LinkedTreeMap){ // gson
                LinkedTreeMap<String, ?> conditionMap = (LinkedTreeMap<String, ?>)rawObjectList.get(i);
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                        (String)conditionMap.get("match"), conditionMap.get("value")));
            }
            else {
                Map<String, ?> conditionMap = (Map<String, ?>)rawObjectList.get(i);
                conditions.add(new UserAttribute((String)conditionMap.get("name"), (String)conditionMap.get("type"),
                        (String)conditionMap.get("match"), conditionMap.get("value")));
            }
        }

        Condition condition;
        switch (operand) {
            case "and":
                condition = new AndCondition(conditions);
                break;
            case "or":
                condition = new OrCondition(conditions);
                break;
            case "not":
                condition = new NotCondition(conditions.get(0));
                break;
            default:
                condition = new OrCondition(conditions);
                break;
        }

        return condition;
    }

}
