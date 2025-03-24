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

import static explorableviz.transparenttext.Query.loadQueries;

public class InContextLearning {
    private final String systemPrompt;

    private final ArrayList<Query> cases;

    public InContextLearning(String systemPrompt, ArrayList<Query> cases) {
        this.systemPrompt = systemPrompt;
        this.cases = cases;
    }

    public static InContextLearning loadLerningCases(String jsonLearningCasePath, int numCasesToGenerate) throws Exception {
        ArrayList<Query> learningCases = loadQueries(Settings.getLearningCaseFolder(), numCasesToGenerate);
        return new InContextLearning(loadSystemPrompt(jsonLearningCasePath), learningCases);
    }

    public PromptList toPromptList() {
        PromptList inContextLearning = new PromptList();
        inContextLearning.addSystemPrompt(this.systemPrompt);
        for (Query query : this.cases) {
            for(int i = 0; i < query.getParagraph().getParagraphForQueries().size(); i++)
                inContextLearning.addPairPrompt(query.toUserPrompt(i), (((Expression) query.getParagraph().get(1)).getExpr()));
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
