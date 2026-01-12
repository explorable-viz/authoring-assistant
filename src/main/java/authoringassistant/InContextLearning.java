package authoringassistant;

import it.unisa.cluelab.lllm.llm.prompt.PromptList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class InContextLearning {
    private final String systemPrompt;

    public InContextLearning(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public static InContextLearning loadLearningCases(String systemPromptPath) throws Exception {
        return new InContextLearning(loadSystemPrompt(systemPromptPath));
    }

    public PromptList toPromptList() {
        PromptList promptList = new PromptList();
        promptList.addSystemPrompt(this.systemPrompt);
        return promptList;
    }

    public static String loadSystemPrompt(String directoryPath) throws IOException {
        Path systemPromptPath = Paths.get(directoryPath, "system-prompt.txt");
        String systemPrompt = Files.exists(systemPromptPath) ? STR."\{Files.readString(systemPromptPath)}\n" : "";
        List<String> fluidFileContents = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            List<Path> fluidFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("system-prompt.txt")).toList();
            for (Path file : fluidFiles) {
                fluidFileContents.add(Files.readString(file));
            }
        }
        return STR."\{systemPrompt}\n\{String.join("\n", fluidFileContents)}";
    }
}
