package authoringassistant;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Settings {
    public static final String LOG_FOLDER = "logs/";
    public static final String FLUID_TEMP_FOLDER = "./fluid-temp";
    public static final String BASE_PATH_LIBRARY = "node_modules/@explorable-viz/fluid/dist/fluid/fluid";
    public static final String FLUID_COMMON_FOLDER = "./testCases-aux";
    public static final String SYSTEM_PROMPT_PATH = "system-prompt/interpretation-agent";

    private static Settings instance;
    private final JSONObject settings;
    private static Map<String, String> commandLineArgs;

    private static Settings getInstance() {
        if (instance == null) throw new AssertionError("You have to call init first");
        return instance;
    }

    public static Settings init(String settingsPath, Map<String, String> args) throws IOException {
        commandLineArgs = args;
        instance = new Settings(settingsPath);
        return instance;
    }

    public static Settings init(String settingsPath) throws IOException {
        return init(settingsPath, null);
    }

    private Settings(String settingsPath) throws IOException {
        this.settings = new JSONObject(new String(Files.readAllBytes(Paths.get(new File(settingsPath).toURI()))));
    }

    private static JSONObject getSettings() {
        return getInstance().settings;
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

    public static int getNumContextToken() {
        return getSettings().getInt("num-context-token");
    }

    public static boolean isAddExpectedValue() {
        return getSettings().getBoolean("add-expected-value");
    }

    // TODO: don't treat as a "setting"
    public static void setAddExpectedValue(boolean value) {
        getSettings().put("add-expected-value", value);
    }

    public static String getTestCaseFolder() {
        return commandLineArgs.get("test-case-folder");
    }

    public static int numTestRuns() {
        return getSettings().getInt("num-test-runs");
    }

    public static String getSuggestionAgentName() {
        return commandLineArgs.get("suggestion-agent-class");
    }
    public static String getAuthoringAgentName() {
        return commandLineArgs.get("authoring-agent-class");
    }
}