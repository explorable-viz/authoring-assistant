package explorableviz.authoringassistant;


import explorableviz.authoringassistant.paragraph.Literal;
import explorableviz.authoringassistant.paragraph.Paragraph;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@SpringBootApplication
public class AuthoringServer {

    private final InContextLearning inContextLearning;
    public AuthoringServer() throws Exception {
        Settings.init("settings.json");
        inContextLearning = InContextLearning.loadLearningCases(Settings.getSystemPromptPath(), Settings.getNumLearningCaseToGenerate());

    }
    @CrossOrigin(origins = "*")
    @PostMapping("/generate")
    public String generate(@RequestBody FluidGeneratorRequest request) throws Exception {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Literal(request.getText(), null));
        Map<String,String> datasets = new HashMap<>();
        datasets.put("data1", "data1");
        Map<String,String> loadedDatasets = new HashMap<>();
        loadedDatasets.put("data1", request.getDataset());
        AuthoringAssistant authoringAssistant = getAuthoringAssistant(paragraph, datasets, loadedDatasets);
        List<Pair<Program, Program.QueryResult>> result = authoringAssistant.executePrograms();
        return result.getLast().getFirst().getParagraph().toFluidSyntax(false);
    }

    @NotNull
    private AuthoringAssistant getAuthoringAssistant(Paragraph paragraph, Map<String, String> datasets, Map<String, String> loadedDatasets) throws Exception {
        List<String> imports = new ArrayList<>();
        imports.add("scigen");
        imports.add("util");
        String code = "";
        Program program = new Program(paragraph, datasets, imports, code, loadedDatasets, "web-case", new ArrayList<>());
        return new AuthoringAssistant(inContextLearning, Settings.getAuthoringAgentName(), program, Settings.getRecognitionAgentName(), 1);
    }

    public static void main(String[] args) {
        SpringApplication.run(AuthoringServer.class, args);
    }



    public static class FluidGeneratorRequest {
        private String text;
        private String dataset; // può essere Map<String, Object> per più controllo

        public String getText() {
            return text;
        }
        public void setText(String text) {
            this.text = text;
        }

        public String getDataset() {
            return dataset;
        }
        public void setDataset(String dataset) {
            this.dataset = dataset;
        }
    }

}
