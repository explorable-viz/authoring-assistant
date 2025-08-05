package explorableviz.authoringassistant.llm;

import explorableviz.authoringassistant.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.agents.generic.OLLAMAEvaluatorAgent;
import org.json.JSONObject;

public class DeepSeekAgent extends OLLAMAEvaluatorAgent<Expression> {

    public DeepSeekAgent(JSONObject setttings) {
        super(setttings);
        setModel("deepseek-coder:33b");
    }

    @Override
    public Expression parse(String s) {
        return new Expression(s, null, null);
    }
}
