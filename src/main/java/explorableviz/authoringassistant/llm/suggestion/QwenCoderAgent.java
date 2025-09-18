package explorableviz.authoringassistant.llm.recognition;

import it.unisa.cluelab.lllm.llm.agents.generic.OLLAMAEvaluatorAgent;
import org.json.JSONObject;

public class QwenCoderAgent extends OLLAMAEvaluatorAgent<String> {

    public QwenCoderAgent(JSONObject setttings) {
        super(setttings);
        setModel("qwen2.5-coder:32b");
    }

    @Override
    public String parse(String s) {
        return s;
    }
}
