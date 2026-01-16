package authoringassistant.llm;

import authoringassistant.Settings;
import authoringassistant.llm.prompt.PromptList;
import authoringassistant.paragraph.Expression;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class LLMEvaluatorAgent<E> {
    public LLMEvaluatorAgent() {
    }

    public abstract E evaluate(PromptList var1, String var2) throws IOException, InterruptedException;

    public static <E> LLMEvaluatorAgent<E> initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        LLMEvaluatorAgent<E> llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent<E>) agentClass
                .getDeclaredConstructor(Settings.class)
                .newInstance(Settings.getInstance());

        return llmAgent;
    }
}
