package authoringassistant.llm.interpretation;

import authoringassistant.llm.agents.generic.OllamaEvaluatorAgent;
import authoringassistant.paragraph.Expression;
import org.json.JSONObject;

public class DeepSeekAgent extends OllamaEvaluatorAgent<Expression> {

    public DeepSeekAgent(JSONObject settings) {
        super(settings);
        setModel("deepseek-coder:33b");
    }

    @Override
    public Expression parse(String s) {
        return new Expression(s, null, null);
    }
}
