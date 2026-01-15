package authoringassistant.llm;

import authoringassistant.llm.prompt.PromptList;
import java.io.IOException;
import org.json.JSONObject;

public abstract class LLMEvaluatorAgent<E> {
    private JSONObject settings = new JSONObject();

    public LLMEvaluatorAgent(JSONObject settings) {
        if (settings != null) {
            this.settings = settings;
        }

    }

    public abstract E evaluate(PromptList var1, String var2) throws IOException;
}
