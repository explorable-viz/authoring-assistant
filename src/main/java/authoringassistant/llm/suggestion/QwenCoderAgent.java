package authoringassistant.llm.suggestion;

import authoringassistant.Settings;
import authoringassistant.llm.agents.generic.OllamaEvaluatorAgent;
import org.json.JSONObject;

public class QwenCoderAgent extends OllamaEvaluatorAgent<String> {

    public QwenCoderAgent(Settings settings) {
        super(settings);
        setModel("qwen2.5-coder:32b");
    }

    @Override
    public String parse(String s) {
        return s;
    }
}
