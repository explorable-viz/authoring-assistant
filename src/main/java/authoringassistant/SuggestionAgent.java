package authoringassistant;

import authoringassistant.paragraph.Expression;
import authoringassistant.paragraph.ExpressionCategory;
import authoringassistant.paragraph.Literal;
import authoringassistant.paragraph.Paragraph;
import authoringassistant.llm.LLMEvaluatorAgent;
import authoringassistant.llm.prompt.PromptList;
import kotlin.Pair;

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

    public record SuggestionAgentResult(Program program, int attempts) {}

    private static final Pattern REPLACE = Pattern.compile("\\[REPLACE value=\"?(.*?)\"? categories=\"?(.*?)\"?]");
    private static final Path SYSTEM_PROMPT_PATH = Path.of("system-prompt", "suggestion-agent", "system-prompt.txt");
    private final LLMEvaluatorAgent<String> llm;

    public SuggestionAgent(String agent) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        llm = initialiseAgent(agent);
    }

    public SuggestionAgentResult generateTemplateProgram(Program p) throws IOException {
        String text;
        if(p.getTestCaseFileName().equals("web-case")) {
            text = p.getParagraph().getFirst().getValue();
        } else
        {
            text = extractText(p);
        }
        PromptList prompts = buildPrompts(text);
        String result;
        int attempts = 0;
        while(attempts < Settings.getSuggestionAgentLoopbackLimit()) {
            attempts++;
            try {
                result = llm.evaluate(prompts, null);
                if (result == null) {
                    return new SuggestionAgentResult(p, attempts);
                }
                Paragraph paragraph = parseParagraph(result);
                if(!p.getParagraph().equals(paragraph)) {
                    prompts.addUserPrompt("Your response contains extra text outside the annotated paragraph. Please provide ONLY the original text with [REPLACE value=\"...\" categories=\"...\"] annotations inserted inline. Do not add any explanations, comments, markdown formatting, or other additional content.");
                    continue;
                }
                return new SuggestionAgentResult(new Program(paragraph, p.getDatasets(),p.getImports(),p.getCode(),p.get_loadedDatasets(),p.getTestCaseFileName(),p.getTest_datasets()), attempts);
            } catch (IllegalArgumentException ex) {
                prompts.addUserPrompt(STR."Invalid category! Please use only the following categories: \{ExpressionCategory.values()}. Return ONLY the annotated paragraph with [REPLACE ...] tags, without any additional comments or explanations.");
                continue;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Limite di tentativi raggiunto, restituisci il programma originale
        return new SuggestionAgentResult(p, attempts);

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
            HashSet<ExpressionCategory> categories = new HashSet<>();
            // Split by comma to handle multiple categories
            String categoriesStr = matcher.group(2);
            for (String category : categoriesStr.split(",")) {
                categories.add(ExpressionCategory.of(category.trim()));
            }
            paragraph.add(new Expression(STR."\"\{exprValue}\"", exprValue, categories));

            lastIndex = matcher.end();
        }

        // Eventuale literal finale
        if (lastIndex < text.length()) {
            paragraph.add(new Literal(text.substring(lastIndex), null));
        }

        return paragraph;
    }

    private static String extractText(Program p) throws IOException {
        return !p.asIndividualProblems(p).isEmpty() ? p.asIndividualProblems(p)
                .getFirst()
                .component1()
                .getParagraph()
                .getFirst()
                .getValue() : p.getParagraph().getFirst().getValue();
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


    private static LLMEvaluatorAgent<String> initialiseAgent(String agentClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        LLMEvaluatorAgent<String> llmAgent;
        Class<?> agentClass = Class.forName(agentClassName);
        llmAgent = (LLMEvaluatorAgent<String>) agentClass
                .getDeclaredConstructor(Settings.class)
                .newInstance(Settings.getInstance());

        return llmAgent;
    }
}
