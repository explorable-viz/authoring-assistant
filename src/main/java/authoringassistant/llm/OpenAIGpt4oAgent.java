package authoringassistant.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.FunctionExecutor;
import com.theokanning.openai.service.OpenAiService;
import authoringassistant.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.agents.generic.OpenAIEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Collections;

public class OpenAIGpt4oAgent extends OpenAIEvaluatorAgent<Expression> {
    public OpenAIGpt4oAgent(JSONObject settings) {
        super(settings);
        setModel("gpt-4o");
    }


    public Expression evaluate(PromptList prompts, String grid) {
        OpenAiService service = new OpenAiService(getToken(), Duration.ofSeconds(90));
        ChatMessage responseMessage = service.createChatCompletion(getChatCompletionRequest(prompts)).getChoices().get(0).getMessage();
        ChatFunctionCall functionCall = responseMessage.getFunctionCall();
        return getFunctionExecutor().execute(functionCall);
    }

    @Override
    public String functionName() {
        return "generate_fluid_expression";
    }

    @NotNull
    @Override
    public FunctionExecutor getFunctionExecutor() {
        return new FunctionExecutor(Collections.singletonList(ChatFunction.builder()
                .name("generate_fluid_expression")
                .description("Generate an Expression for Fluid")
                .executor(ExpressionResponse.class, exp -> new Expression(exp.expression, "", null))
                .build()));
    }

    static class ExpressionResponse {
        @JsonPropertyDescription("Fluid expression, for example: (record.emissions / sum(map (fun x -> x.emissions) (getByYear year tableData))) * 100")
        public String expression;
    }

}
