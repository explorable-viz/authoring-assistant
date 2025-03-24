package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static explorableviz.transparenttext.Program.loadQueries;

public class InContextLearning {
    private final String systemPrompt;

    private final ArrayList<Program> cases;

    public InContextLearning(String systemPrompt, ArrayList<Program> cases) {
        this.systemPrompt = systemPrompt;
        this.cases = cases;
    }

    public static InContextLearning loadLearningCases(String jsonLearningCasePath, int numCasesToGenerate) throws Exception {
        ArrayList<Program> learningCases = loadQueries(Settings.getLearningCaseFolder(), numCasesToGenerate);
        return new InContextLearning(loadSystemPrompt(jsonLearningCasePath), learningCases);
    }

    public PromptList toPromptList() {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addSystemPrompt(this.systemPrompt);
        for (Program program : this.cases) {
            List<Query> subQueries = program.toSubQueries();
            for (Query query : subQueries)
                inContextLearning.addPairPrompt(query.toUserPrompt(), (((Expression) program.getParagraph().get(1)).getExpr()));
        }
        return inContextLearning;
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

    public int size() {
        return this.cases.size();
    }
}
