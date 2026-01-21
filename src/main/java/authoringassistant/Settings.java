package authoringassistant;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Settings {
    public static final String FLUID_TEMP_FOLDER = "./fluid-temp";
    public static final String BASE_PATH_LIBRARY = "node_modules/@explorable-viz/fluid/dist/fluid/fluid";
    public static final String FLUID_COMMON_FOLDER = "./testCases-aux";
    public static final String SYSTEM_PROMPT_PATH = "system-prompt/interpretation-agent";

    private static Settings instance;
    private final JSONObject settings;
    private static Map<String, String> commandLineArgs;
    private static String configName;

    public static Settings getInstance() {
        if (instance == null) throw new AssertionError("You have to call init first");
        return instance;
    }

    public static Settings init(String settingsPath, Map<String, String> args) throws IOException {
        commandLineArgs = args;

        JSONObject defaultSettings = loadSettingsFile(settingsPath);
        if (args != null && args.containsKey("config")) {
            configName = args.get("config");
            String configFile = STR."settings/\{configName}.json";
            JSONObject specificSettings = loadSettingsFile(configFile);
            defaultSettings = rightBiasedUnion(defaultSettings, specificSettings);
        } else {
            configName = "default";
        }
        
        instance = new Settings(defaultSettings);
        return instance;
    }

    public static Settings init(String settingsPath) throws IOException {
        return init(settingsPath, null);
    }

    private Settings(String settingsPath) throws IOException {
        this.settings = loadSettingsFile(settingsPath);
    }
    
    private Settings(JSONObject settings) {
        this.settings = settings;
    }
    
    private static JSONObject loadSettingsFile(String settingsPath) throws IOException {
        return new JSONObject(new String(Files.readAllBytes(Paths.get(new File(settingsPath).toURI()))));
    }
    
    public static JSONObject rightBiasedUnion(JSONObject left, JSONObject right) {
        JSONObject result = new JSONObject(left.toString()); // defensive copy
        for (String key : right.keySet()) {
            result.put(key, right.get(key));
        }
        return result;
    }

    public static JSONObject getSettings() {
        return getInstance().settings;
    }

    // treat claude-token, gemini-token in the same way to allow for local settings?
    public static String getOpenAIToken() {
        String envVar = "OPENAI_API_KEY";
        final String apiKey = System.getenv(envVar);

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(STR."\{envVar} not set");
        }
        return apiKey;
    }

    public static String getOllamaURL() {
        return getSettings().getString("ollama-url");
    }

    public static int getOllamaPort() {
        return getSettings().getInt("ollama-port");
    }

    public static int getInterpretationAgentLoopbackLimit() {
        return getSettings().getInt("interpretation-agent-loopback-limit");
    }

    public static int getSuggestionAgentLoopbackLimit() {
        return getSettings().getInt("suggestion-agent-loopback-limit");
    }

    public static float getTemperature() {
        return getSettings().getFloat("temperature");
    }

    public static int getTimeout() {
        return getSettings().getInt("timeout");
    }

    // Stop after this maany test cases, or -1 to run all.
    public static int getTruncateTestsAt() {
        return getSettings().getInt("truncate-tests-at");
    }

    // TODO: rename property now all uses factor through this method
    public static int getNumContextToken() {
        return getSettings().getInt("num_ctx");
    }

    public static boolean isAddExpectedValue() {
        return getSettings().getBoolean("add-expected-value");
    }

    // TODO: don't treat as a "setting"
    public static void setAddExpectedValue(boolean value) {
        getSettings().put("add-expected-value", value);
    }

    public static String getTestCaseFolder() {
        return getSettings().getString("test-case-folder");
    }
    
    public static String getConfigName() {
        return configName;
    }

    public static int numTestRuns() {
        return getSettings().getInt("num-test-runs");
    }

    public static String getSuggestionAgentName() {
        return getSettings().optString("suggestion-agent-class", null);
    }
    
    public static String getAuthoringAgentName() {
        return getSettings().optString("authoring-agent-class", null);
    }
}
