package authoringassistant.llm.suggestion;

import authoringassistant.Settings;
import authoringassistant.llm.agents.generic.OllamaEvaluatorAgent;
import org.json.JSONObject;

public class CodeLlamaAgent extends OllamaEvaluatorAgent<String> {

    public CodeLlamaAgent(Settings settings) {
        super(settings);
        setModel("codellama:34b");
    }

    @Override
    public String parse(String s) {
        return s;
    }
}
