package authoringassistant.llm.agents.generic;

import authoringassistant.Settings;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.Prompt;
import authoringassistant.llm.prompt.PromptList;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class OllamaEvaluatorAgent<E> extends LLMEvaluatorAgent<E> {
    public static Logger logger = Logger.getLogger(OllamaEvaluatorAgent.class.getName());
    private final String urlLlama;
    private final int port;
    private final double temperature;
    private final int timeout;
    private final double ctx;
    private String model;
    private OkHttpClient client;
    private static final String endPointGenerate = "/api/chat";

    public OllamaEvaluatorAgent(Settings settings) {
        this.urlLlama = settings.getOllamaURL();
        this.port = settings.getOllamaPort();
        this.temperature = settings.getTemperature();
        this.ctx = settings.getNumContextToken();
        this.timeout = settings.getTimeout() ;

        this.initClient();
    }

    public void setModel(String model) {
        this.model = model;
    }

    public E evaluate(PromptList prompts, String grid) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject prompt = new JSONObject();
        JSONObject options = new JSONObject();
        prompt.put("model", this.model);
        options.put("temperature", this.temperature);
        options.put("num_ctx", this.ctx);
        prompt.put("options", options);
        prompt.put("system", ((Prompt)prompts.get(0)).getContent());
        prompt.put("stream", false);
        JSONArray messages = new JSONArray();
        prompt.put("messages", messages);

        for(int i = 0; i < prompts.size(); ++i) {
            Prompt p = (Prompt)prompts.get(i);
            JSONObject message = new JSONObject();
            message.put("role", p.getRole());
            message.put("content", p.getContent());
            messages.put(message);
        }

        RequestBody body = RequestBody.create(mediaType, prompt.toString());
        Request request = (new Request.Builder()).url(this.urlLlama + ":" + this.port + "/api/chat").method("POST", body).addHeader("Content-Type", "application/json").build();
        Response response = this.client.newCall(request).execute();
        if (response.body() != null) {
            String responseJson = response.body().string();
            logger.info(responseJson);
            JSONObject resp = new JSONObject(responseJson);
            JSONObject message = (JSONObject)resp.get("message");
            prompts.addAssistantPrompt(message.get("content").toString());
            return (E)this.parse((String)message.get("content"));
        } else {
            System.out.println("null-response");
            return null;
        }
    }

    public abstract E parse(String var1);

    public void initClient() {
        this.client = (new OkHttpClient()).newBuilder().connectTimeout((long)this.timeout, TimeUnit.SECONDS).writeTimeout((long)this.timeout, TimeUnit.SECONDS).readTimeout((long)this.timeout, TimeUnit.SECONDS).build();
    }
}
