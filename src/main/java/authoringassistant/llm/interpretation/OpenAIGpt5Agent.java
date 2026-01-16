package authoringassistant.llm.interpretation;

import authoringassistant.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;
import authoringassistant.paragraph.Expression;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.Prompt;
import authoringassistant.llm.prompt.PromptList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

public class OpenAIGpt5Agent extends LLMEvaluatorAgent<Expression> {
    public static Logger logger = Logger.getLogger(OpenAIGpt5Agent.class.getName());
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final double temperature;
    private final int ctx;

    public OpenAIGpt5Agent(Settings settings) {
        this.token = settings.getOpenAIToken();
        this.temperature = settings.getTemperature();
        this.ctx = settings.getNumContextToken();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Expression evaluate(PromptList prompts, String grid) throws IOException, InterruptedException {
        logger.config("Execution of the OpenAIGpt5Agent");
        
        try {
            JSONObject requestBody = buildRequestBody(prompts);
            String response = makeHttpRequest(requestBody);
            return parseResponse(response);
            
        } catch (IOException | InterruptedException e) {
            logger.severe("Error calling GPT-5 API: " + e.getMessage());
            throw e;
        }
    }

    public String getModel() {
        return "gpt-5-mini";
    }
    
    private JSONObject buildRequestBody(PromptList prompts) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", this.getModel());
        requestBody.put("temperature", this.temperature);
        requestBody.put("max_completion_tokens", this.ctx);
        
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an expert in Fluid programming language. Generate valid Fluid expressions based on the given context.");
        messages.put(systemMessage);
        
        for (Prompt prompt : prompts) {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt.getContent());
            messages.put(message);
        }
        
        requestBody.put("messages", messages);
        
        JSONArray functions = new JSONArray();
        JSONObject function = new JSONObject();
        function.put("name", "generate_fluid_expression");
        function.put("description", "Generate an Expression for Fluid");
        
        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        JSONObject properties = new JSONObject();
        JSONObject expressionProp = new JSONObject();
        expressionProp.put("type", "string");
        expressionProp.put("description", "Fluid expression, for example: (record.emissions / sum(map (fun x -> x.emissions) (getByYear year tableData))) * 100");
        properties.put("expression", expressionProp);
        parameters.put("properties", properties);
        parameters.put("required", new JSONArray().put("expression"));
        function.put("parameters", parameters);
        
        functions.put(function);
        requestBody.put("functions", functions);
        requestBody.put("function_call", new JSONObject().put("name", "generate_fluid_expression"));
        
        return requestBody;
    }
    
    private String makeHttpRequest(JSONObject requestBody) throws IOException, InterruptedException {
        String requestBodyString = requestBody.toString();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
        }
        
        return response.body();
    }
    
    private Expression parseResponse(String responseBody) throws IOException {
        JSONObject response = new JSONObject(responseBody);
        JSONArray choices = response.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        
        if (message.has("function_call")) {
            JSONObject functionCall = message.getJSONObject("function_call");
            String arguments = functionCall.getString("arguments");
            JSONObject args = new JSONObject(arguments);
            String expression = args.getString("expression");
            return new Expression(expression, "", null);
        } else {
            String content = message.getString("content");
            return new Expression(content, "", null);
        }
    }
}
