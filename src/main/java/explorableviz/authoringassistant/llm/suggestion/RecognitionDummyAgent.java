package explorableviz.authoringassistant.llm.suggestion;

import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class RecognitionDummyAgent extends LLMEvaluatorAgent<String> {
    public static Logger logger = Logger.getLogger(RecognitionDummyAgent.class.getName());
    public RecognitionDummyAgent(JSONObject settings) {
        super(settings);
    }

    @Override
    public String evaluate(PromptList list, String s) throws IOException {
        logger.info("Execution of the DummyAgent");
        list.addSystemPrompt("dummy");
        return "Test paragraph with one replace tag with value [REPLACE value=\"10\"].";
    }
}
