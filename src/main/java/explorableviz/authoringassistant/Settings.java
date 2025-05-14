package explorableviz.authoringassistant;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class Settings {

    private static Settings instance;
    private final JSONObject settings;

    private static Settings getInstance() {
        if (instance == null) throw new AssertionError("You have to call init first");
        return instance;
    }

    public static Settings init(String settingsPath) throws IOException {
        instance = new Settings(settingsPath);
        return instance;
    }

    private Settings(String settingsPath) throws IOException {
        this.settings = new JSONObject(new String(Files.readAllBytes(Paths.get(new File(settingsPath).toURI()))));
    }

    public static JSONObject getSettings() {
        return getInstance().settings;
    }

    public static int getLimit() {
        return getSettings().getInt("agent-limit");
    }

    public static String getFluidTempFolder() {
        return getSettings().getString("fluid-temp-folder");
    }

    public static String getLibrariesBasePath() {
        return getSettings().getString("base-path-library");
    }

    public static String getFluidCommonFolder() {
        return getSettings().getString("fluid-common-folder");
    }

    public static float getTemperature() {
        return getSettings().getFloat("temperature");
    }

    public static int getNumContextToken() {
        return getSettings().getInt("num_ctx");
    }

    public static String getLogFolder() {
        return getSettings().getString("log-folder");
    }

    public static boolean isReasoningEnabled() {
        return getSettings().getBoolean("enable-reasoning");
    }
    public static boolean isSplitMultipleTagEnabled() {
        return getSettings().getBoolean("split-multiple-replace-tag");
    }public static boolean isAddExpectedValueEnabled() {
        return getSettings().getBoolean("add-expected-value");
    }

    public static String getLearningCaseFolder() {
        return getSettings().getString("learning-case-folder");
    }

    public static String getTestCaseFolder() {
        return getSettings().getString("test-case-folder");
    }

    public static float getThreshold() {
        return getSettings().getFloat("threshold");
    }

    public static String getSystemPromptPath() {
        return getSettings().getString("system-prompt-path");
    }

    public static int getNumTestToGenerate() {
        return getSettings().getInt("num-test-to-generate");
    }

    public static int getNumTestDataVariants() {
        return getSettings().getInt("num-test-data-variants");
    }

    public static int getNumLearningCaseToGenerate() {
        return getSettings().getInt("num-learning-case-to-generate");
    }

    public static Optional<Integer> getNumQueryToExecute() {
        return (getSettings().has("num-query-to-execute") && !getSettings().isNull("num-query-to-execute")) ? Optional.of(getSettings().getInt("num-query-to-execute")) : Optional.empty();
    }

    public static boolean isEditorLoopEnabled() {
        return getSettings().getBoolean("enable-editor-loop");
    }
}
