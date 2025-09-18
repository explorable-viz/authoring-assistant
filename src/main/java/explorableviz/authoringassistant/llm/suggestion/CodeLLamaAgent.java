package explorableviz.authoringassistant.llm.suggestion;

import it.unisa.cluelab.lllm.llm.agents.generic.OLLAMAEvaluatorAgent;
import org.json.JSONObject;

public class CodeLLamaAgent extends OLLAMAEvaluatorAgent<String> {

    public CodeLLamaAgent(JSONObject setttings) {
        super(setttings);
        setModel("codellama:34b");
    }

    @Override
    public String parse(String s) {
        return s;
    }
}
