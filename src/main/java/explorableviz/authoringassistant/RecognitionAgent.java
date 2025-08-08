package explorableviz.authoringassistant;

import explorableviz.authoringassistant.llm.recognition.CodeLLamaAgent;
import explorableviz.authoringassistant.paragraph.Expression;
import explorableviz.authoringassistant.paragraph.Literal;
import explorableviz.authoringassistant.paragraph.Paragraph;
import it.unisa.cluelab.lllm.llm.LLMEvaluatorAgent;
import it.unisa.cluelab.lllm.llm.prompt.PromptList;
import kotlin.Pair;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class RecognitionAgent {

    private static final Pattern REPLACE = Pattern.compile("\\[REPLACE value=\"?(.*?)\"?]");
    private static final Path SYSTEM_PROMPT_PATH = Path.of("rec-agent", "system-prompt.txt");
    private final LLMEvaluatorAgent<String> llm;

    public RecognitionAgent(String agent) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        llm = initialiseAgent(agent);
    }

    public List<Pair<Program, Expression>> function(Program p) throws IOException {
        String text = extractText(p);
        PromptList prompts = buildPrompts(text);
        String result = llm.evaluate(prompts, null);

        List<Pair<Paragraph, Expression>>  paragraphs = generateParagraphs(result);
        List<Pair<Program, Expression>> programs = new ArrayList<>();
        for (Pair<Paragraph, Expression> paragraph : paragraphs) {
            programs.add(new Pair<>(new Program(paragraph.getFirst(), p.getDatasets(), p.getImports(), p.getCode(), p.get_loadedDatasets(), p.getTestCaseFileName(), p.getTest_datasets()), paragraph.getSecond()));
        }
        return programs;
    }

    private static String extractText(Program p) throws IOException {
        return p.asIndividualEdits(p)
                .getFirst()
                .component1()
                .getParagraph()
                .getFirst()
                .getValue();
    }

    private static PromptList buildPrompts(String userText) throws IOException {
        PromptList prompts = new PromptList();
        String systemPrompt = Files.readString(RecognitionAgent.SYSTEM_PROMPT_PATH, StandardCharsets.UTF_8);
        prompts.addSystemPrompt(systemPrompt);
        prompts.addUserPrompt(userText);
        return prompts;
    }


    public static List<Pair<Paragraph, Expression>> generateParagraphs(String input) {
        Pair<String, List<Literal.SelectedRegion>> build = buildFullTextAndRegions(input, parsePlaceholders(input));
        return toParagraphPairs(build.getFirst(), build.getSecond());
    }

    private static List<Pair<Pair<Integer, Integer>, String>> parsePlaceholders(String input) {
        return REPLACE.matcher(input)
                .results()
                .map(mr -> new Pair<>(
                        new Pair<>(mr.start(), mr.end()),
                        mr.group(1)
                ))
                .toList();
    }

    private static Pair<String, List<Literal.SelectedRegion>> buildFullTextAndRegions(
            String input,
            List<Pair<Pair<Integer, Integer>, String>> matches
    ) {
        StringBuilder sb = new StringBuilder(input.length());
        List<Literal.SelectedRegion> regions = new ArrayList<>(matches.size());

        int last = 0;
        for (Pair<Pair<Integer, Integer>, String> m : matches) {
            int startIdx = m.getFirst().getFirst();
            int endIdx = m.getFirst().getSecond();
            String value = m.getSecond();

            sb.append(input, last, startIdx);
            int start = sb.length();
            sb.append(value);
            int end = sb.length();
            regions.add(new Literal.SelectedRegion(start, end));

            last = endIdx;
        }
        sb.append(input, last, input.length());

        return new Pair<>(sb.toString(), regions);
    }

    private static List<Pair<Paragraph, Expression>> toParagraphPairs(
            String fullText, List<Literal.SelectedRegion> regions
    ) {
        return regions.stream()
                .map(region -> {
                    Paragraph paragraph = new Paragraph();
                    String value = fullText.substring(region.start(), region.end());
                    paragraph.add(new Literal(fullText, region));

                    Expression expr = new Expression(STR."\"\{value}\"", value, new HashSet<>());
                    return new Pair<>(paragraph, expr);
                })
                .toList();
    }


    private LLMEvaluatorAgent<String> initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        LLMEvaluatorAgent<String> llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent<String>) agentClass
                .getDeclaredConstructor(JSONObject.class)
                .newInstance(Settings.getSettings());

        return llmAgent;
    }
}
