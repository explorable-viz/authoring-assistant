package authoringassistant.llm;

import authoringassistant.llm.prompt.PromptList;
import java.io.IOException;

public abstract class LLMEvaluatorAgent<E> {
    public LLMEvaluatorAgent() {
    }

    public abstract E evaluate(PromptList var1, String var2) throws IOException, InterruptedException;
}
