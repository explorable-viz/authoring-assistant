package explorableviz.authoringassistant.llm;

import explorableviz.authoringassistant.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.Prompt;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class LLMDummyAgent extends LLMEvaluatorAgent<Expression> {
    public static Logger logger = Logger.getLogger(LLMDummyAgent.class.getName());
    public LLMDummyAgent(JSONObject settings) {
        super(settings);
    }

    @Override
    public Expression evaluate(PromptList list, String s) throws IOException {
        logger.info("Execution of the DummyAgent");
        list.addSystemPrompt("dummy");
        return new Expression("dummy", null, null);
    }
}
