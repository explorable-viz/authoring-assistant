package authoringassistant.llm.suggestion;

import it.unisa.cluelab.lllm.llm.agents.generic.OLLAMAEvaluatorAgent;
import org.json.JSONObject;

public class CodeLlamaAgent extends OLLAMAEvaluatorAgent<String> {

    public CodeLlamaAgent(JSONObject settings) {
        super(settings);
        setModel("codellama:34b");
    }

    @Override
    public String parse(String s) {
        return s;
    }
}
