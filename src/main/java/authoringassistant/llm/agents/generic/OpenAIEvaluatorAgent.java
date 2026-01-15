package authoringassistant.llm.agents.generic;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.service.FunctionExecutor;
import com.theokanning.openai.service.OpenAiService;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.Prompt;
import authoringassistant.llm.prompt.PromptList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public abstract class OpenAIEvaluatorAgent<E> extends LLMEvaluatorAgent<E> {
    private String model;
    private final String token;
    private final double temperature;
    private final int ctx;

    public OpenAIEvaluatorAgent(JSONObject settings) {
        super(settings);
        this.token = settings.getString("openai-token");
        this.temperature = (double)settings.getFloat("temperature");
        this.ctx = settings.getInt("num_ctx");
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String functionName() {
        return "auto";
    }

    public E evaluate(PromptList prompts, String grid) {
        OpenAiService service = new OpenAiService(this.getToken(), Duration.ofSeconds(90L));
        ChatMessage responseMessage = ((ChatCompletionChoice)service.createChatCompletion(this.getChatCompletionRequest(prompts)).getChoices().get(0)).getMessage();
        ChatFunctionCall functionCall = responseMessage.getFunctionCall();
        return (E)responseMessage.getContent();
    }

    public ChatCompletionRequest getChatCompletionRequest(List<Prompt> prompts) {
        return ChatCompletionRequest.builder().model(this.model).temperature(this.temperature).messages(toChatMessages(prompts)).functions(this.getFunctionExecutor().getFunctions()).functionCall(ChatCompletionRequestFunctionCall.of(this.functionName())).n(1).maxTokens(this.ctx).logitBias(new HashMap()).build();
    }

    @NotNull
    public FunctionExecutor getFunctionExecutor() {
        return new FunctionExecutor(Collections.singletonList(ChatFunction.builder().name("transpile").description("Convert a string").executor(Result.class, OpenAIEvaluatorAgent.Result::getAnswer).build()));
    }

    @NotNull
    private static List<ChatMessage> toChatMessages(List<Prompt> prompts) {
        List<ChatMessage> messages = new ArrayList();

        for(int i = 0; i < prompts.size(); ++i) {
            if (((Prompt)prompts.get(i)).getRole().equals("system")) {
                ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), ((Prompt)prompts.get(i)).getContent());
                messages.add(systemMessage);
            } else if (((Prompt)prompts.get(i)).getRole().equals("user")) {
                ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER.value(), ((Prompt)prompts.get(i)).getContent());
                messages.add(firstMsg);
            } else {
                ChatMessage firstMsg = new ChatMessage(ChatMessageRole.ASSISTANT.value(), ((Prompt)prompts.get(i)).getContent());
                messages.add(firstMsg);
            }
        }

        return messages;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public String getToken() {
        return this.token;
    }

    public String getModel() {
        return this.model;
    }

    public int getCtx() {
        return this.ctx;
    }

    class Result {
        String answer;

        Result() {
        }

        public String getAnswer() {
            return this.answer;
        }
    }
}
