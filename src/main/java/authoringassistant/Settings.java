package authoringassistant;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Settings {

    // Constants for unchanging configuration values
    public static final String LOG_FOLDER = "logs/";
    public static final String FLUID_TEMP_FOLDER = "./fluid-temp";
    public static final String BASE_PATH_LIBRARY = "node_modules/@explorable-viz/fluid/dist/fluid/fluid";
    public static final String FLUID_COMMON_FOLDER = "./testCases-aux";
    public static final String LEARNING_CASE_FOLDER = "learningCases";
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

    public static JSONObject getSettings() {
        return getInstance().settings;
    }

    // TODO: better name
    public static int getTestLimit() {
        return getSettings().getInt("test-limit");
    }

    public static float getTemperature() {
        return getSettings().getFloat("temperature");
    }

    public static int getNumContextToken() {
        return getSettings().getInt("num_ctx");
    }

    public static boolean isReasoningEnabled() {
        return getSettings().getBoolean("enable-reasoning");
    }
    public static boolean isSplitMultipleTagEnabled() {
        return getSettings().getBoolean("split-multiple-replace-tag");
    }public static boolean isAddExpectedValueEnabled() {
        return getSettings().getBoolean("add-expected-value");
    }

    public static void setAddExpectedValue(boolean value) {
        getSettings().put("add-expected-value", value);
    }

    public static String getTestCaseFolder() {
        return commandLineArgs.get("test-case-folder");
    }

    public static float getThreshold() {
        return getSettings().getFloat("threshold");
    }

    public static int maxProgramVariants() {
        return getSettings().getInt("max-program-variants");
    }

    public static int getNumTestDataVariants() {
        return getSettings().getInt("num-test-data-variants");
    }

    public static int getNumLearningCaseToGenerate() {
        return getSettings().getInt("num-learning-case-to-generate");
    }

    public static int numTestRuns() {
        return getSettings().getInt("num-test-runs");
    }

    public static boolean isEditorLoopEnabled() {
        return getSettings().getBoolean("enable-editor-loop");
    }
    public static boolean isSuggestionAgentEnabled() {
        return getSettings().getBoolean("enable-suggestion-agent");
    }

    public static String getSuggestionAgentName() {
        return commandLineArgs.get("suggestion-agent-class");
    }
    public static String getAuthoringAgentName() {
        return commandLineArgs.get("authoring-agent-class");
    }
}