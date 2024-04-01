package industries.nearcore.rule.engine;

import java.util.LinkedHashMap;
import java.util.Map;

public class ActionHelper {

    public Map<String, Integer> actionItemMap;

    public ActionHelper() {
        actionItemMap = new LinkedHashMap<>();
    }

    public void addAction(String ruleName){
        if(null == actionItemMap.get(ruleName)){
            actionItemMap.put(ruleName, 1);
        }else{
            actionItemMap.put(ruleName , actionItemMap.get(ruleName)+1);
        }
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder("{\n");

        // count the total rule executions
        int totalRuleExecutionCount = actionItemMap.values().stream().mapToInt(Integer::intValue).sum();

        // emit the action item map
        for(Map.Entry<String, Integer> entry : actionItemMap.entrySet()) {
            builder.append("    ").append("\"").append(entry.getKey()).append("\" : ").append(entry.getValue()).append(",\n");
        }

        // emit the total rule execution count
        builder.append("    ").append("\"totalRuleExecutionCount\" : ").append(totalRuleExecutionCount);

        return builder.append("\n}").toString();
    }
}
