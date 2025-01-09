package explorableviz.transparenttext.llm;

import explorableviz.transparenttext.agents.FluidGeneratorAgent;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.Prompt;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class LLMDummyAgent extends LLMEvaluatorAgent {
    public static Logger logger = Logger.getLogger(LLMDummyAgent.class.getName());
    public LLMDummyAgent(JSONObject settings) {
        super(settings);
    }

    @Override
    public String evaluate(List<Prompt> list, String s) throws IOException {
        logger.info("Execution of the DummyAgent");
        return "__dummy response__";
    }
}
