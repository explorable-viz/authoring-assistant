package explorableviz.authoringassistant;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuggestionAgent {

    private static final Pattern REPLACE = Pattern.compile("\\[REPLACE value=\"?(.*?)\"?]");
    private static final Path SYSTEM_PROMPT_PATH = Path.of("suggestion-agent", "system-prompt.txt");
    private final LLMEvaluatorAgent<String> llm;

    public SuggestionAgent(String agent) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        llm = initialiseAgent(agent);
    }

    public Program generateTemplateProgram(Program p) throws IOException {
        String text;
        if(p.getTestCaseFileName().equals("web-case")) {
            text = p.getParagraph().getFirst().getValue();
        } else
        {
            text = extractText(p);
        }
        PromptList prompts = buildPrompts(text);
        String result = llm.evaluate(prompts, null);
        Paragraph paragraph = parseParagraph(result);
        return new Program(paragraph, p.getDatasets(),p.getImports(),p.getCode(),p.get_loadedDatasets(),p.getTestCaseFileName(),p.getTest_datasets());
    }

    private Paragraph parseParagraph(String text) {
        Paragraph paragraph = new Paragraph();
        Matcher matcher = REPLACE.matcher(text);

        int lastIndex = 0;
        while (matcher.find()) {
            // Literal prima del replace
            if (matcher.start() > lastIndex) {
                String literalText = text.substring(lastIndex, matcher.start());
                paragraph.add(new Literal(literalText, null));
            }
            // Expression
            String exprValue = matcher.group(1);
            paragraph.add(new Expression(STR."\"\{exprValue}\"", exprValue, new HashSet<>()));

            lastIndex = matcher.end();
        }

        // Eventuale literal finale
        if (lastIndex < text.length()) {
            paragraph.add(new Literal(text.substring(lastIndex), null));
        }

        return paragraph;
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
        String systemPrompt = Files.readString(SuggestionAgent.SYSTEM_PROMPT_PATH, StandardCharsets.UTF_8);
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
