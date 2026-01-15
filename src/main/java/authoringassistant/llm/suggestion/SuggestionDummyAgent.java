package authoringassistant.llm.suggestion;

import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class SuggestionDummyAgent extends LLMEvaluatorAgent<String> {
    public static Logger logger = Logger.getLogger(SuggestionDummyAgent.class.getName());
    
    public SuggestionDummyAgent(JSONObject settings) {
        super(settings);
    }

    @Override
    public String evaluate(PromptList list, String s) throws IOException {
        logger.config("Execution of the DummyAgent");
        list.addSystemPrompt("dummy");
        return null;
    }
}
