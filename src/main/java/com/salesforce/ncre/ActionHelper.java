package com.salesforce.ncre;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionHelper {

    public  void setActionItemList(Map<String, ActionItem> actionItemList) {
        this.actionItemList = actionItemList;
    }

    public  Map<String, ActionItem> getActionItemList() {
        return actionItemList;
    }


    /*
     * Add an action item to the Context (just print for now)
     */
    public  Map<String, ActionItem> actionItemList;

    public ActionHelper() {
        actionItemList = new HashMap<>();
    }

    public ActionItem addActionItem( String ruleName , String ruleSetName, String actionName) {
        ActionItem ai = actionItemList.get(ruleName+"_"+actionName);
        if(null == ai) {
             ai = new ActionItem(ruleName, ruleSetName, actionName);
            actionItemList.put(ruleName+"_"+actionName , ai);
        }
        return ai;
    }
}
