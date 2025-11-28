package authoringassistant.llm;

import authoringassistant.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class LLMDummyAgent extends LLMEvaluatorAgent<Expression> {
    public static Logger logger = Logger.getLogger(LLMDummyAgent.class.getName());
    public LLMDummyAgent(JSONObject settings) {
        super(settings);
    }

    @Override
    public Expression evaluate(PromptList list, String s) throws IOException {
        logger.config("Execution of the DummyAgent");
        return new Expression("\"dummy\"", null, null);
    }
}
