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
}
