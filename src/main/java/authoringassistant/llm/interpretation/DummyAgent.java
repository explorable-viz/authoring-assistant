package authoringassistant.llm.interpretation;

import authoringassistant.Settings;
import authoringassistant.paragraph.Expression;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class DummyAgent extends LLMEvaluatorAgent<Expression> {
    public static Logger logger = Logger.getLogger(DummyAgent.class.getName());

    public DummyAgent(Settings settings) {
    }

    @Override
    public Expression evaluate(PromptList list, String s) throws IOException {
        logger.config("Execution of the DummyAgent");
        return new Expression("\"dummy\"", null, null);
    }
}
