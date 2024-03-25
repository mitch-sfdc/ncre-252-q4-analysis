package com.salesforce.ncre;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionItem {
    String ruleName;
    String ruleSetName;
    String actionName;
    Map<String, Object> actionParams;

    public ActionItem(String ruleName, String ruleSetName, String actionName) {
        this.ruleName = ruleName;
        this.ruleSetName = ruleSetName;
        this.actionName = actionName;
        actionParams = new HashMap<>();
    }

    public void addActionParam(String key , Object value , boolean isCollection){
        if(isCollection){
            List valList  = (List) actionParams.getOrDefault(key, new ArrayList<>());
            valList.add(value);
            actionParams.put(key,valList );
        }else{
            actionParams.put(key, value);
        }
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleSetName() {
        return ruleSetName;
    }

    public void setRuleSetName(String ruleSetName) {
        this.ruleSetName = ruleSetName;
    }

    public Map<String, Object> getActionParams() {
        return actionParams;
    }

    public void setActionParams(Map<String, Object> actionParams) {
        this.actionParams = actionParams;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
}
