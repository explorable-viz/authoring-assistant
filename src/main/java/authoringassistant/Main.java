package authoringassistant;

import kotlin.Pair;
import authoringassistant.Main.ProgramExpression;
import authoringassistant.Program.QueryResult;
import authoringassistant.util.ThrowingConsumer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static authoringassistant.Program.cleanWebsiteFolders;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {
        Map<String, String> arguments = parseArguments(args);
        logger.config("Arguments passed from command line");
        logger.config(arguments.toString().replace(",", "\n"));
        final ArrayList<Program> programs;
        final InContextLearning inContextLearning;

        try {
            Settings.init("settings.json", arguments);
            final String interpretationAgent = Settings.getAuthoringAgentName();
            final String suggestionAgent = Settings.getSuggestionAgentName();
            //Create directory for logs and json
            cleanWebsiteFolders(STR."website/authoring-assistant/\{Settings.getTestCaseFolder()}/");
            inContextLearning = InContextLearning.loadLearningCases(Settings.SYSTEM_PROMPT_PATH, Settings.getNumLearningCaseToGenerate());
            programs = Program.loadPrograms(Settings.getTestCaseFolder(), Settings.maxProgramVariants());
            if(suggestionAgent != null && interpretationAgent == null) {
                generatePrograms(programs, suggestionAgent, "testCases/scigen-SuggestionAgent");
            }
            else if(arguments.containsKey("downsample") && arguments.get("downsample").equals("true")) {
                int expressionPerCategory = Integer.parseInt(arguments.get("expression-per-category"));
                int sampleSize = Integer.parseInt(arguments.get("sample-size"));
                String outputFolder = STR."testCases/\{Settings.getTestCaseFolder()}-downsampled";
                downsamplePrograms(programs, expressionPerCategory, sampleSize)
                    .forEach(ThrowingConsumer.toConsumer(program -> saveProgramToJson(program, outputFolder)));
            }
            else
            {
                final ArrayList<Pair<Program, QueryResult>> allResults = new ArrayList<>();
                boolean[] cases = interpretationAgent.equals("authoringassistant.llm.LLMDummyAgent") ? new boolean[]{false} : new boolean[]{false, true};
                // Run experiment for both add-expected-value settings
                for (boolean addExpectedValue : cases) {
                    Settings.setAddExpectedValue(addExpectedValue);
                    System.out.println(STR."Running experiment with add-expected-value=\{addExpectedValue}");
                    final ArrayList<Pair<Program, QueryResult>> results = execute(inContextLearning, interpretationAgent, suggestionAgent, programs);
                    allResults.addAll(results);
                }

                float accuracy = computeAccuracy(allResults);
                generateLinks();
                writeLog(allResults, interpretationAgent, inContextLearning.size());
                if (accuracy >= Settings.getThreshold()) {
                    System.out.println(STR."Accuracy OK =\{accuracy}");
                    System.exit(0);
                } else {
                    System.out.println(STR."Accuracy KO =\{accuracy}");
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void saveProgramToJson(Program program, String outputFolder) throws IOException {
        String json = program.toJsonProgram().toString(2);
        String fileName = STR."\{Path.of(program.getTestCaseFileName()).getFileName()}.json";
        Path outputPath = Paths.get(outputFolder, fileName);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, json);
        logger.info(STR."Generated program saved to: \{outputPath}");

        // Create empty .fld file with same name
        String fldFileName = fileName.replace(".json", ".fld");
        Path fldPath = Paths.get(outputFolder, fldFileName);
        Files.writeString(fldPath, "");
        logger.info(STR."Empty .fld file created: \{fldPath}");
    }

    private static void generatePrograms(List<Program> programs, String suggestionAgentClassName, String outputFolder)
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IOException {
        SuggestionAgent sa = new SuggestionAgent(suggestionAgentClassName);
        for (Program program : programs) {
            saveProgramToJson(sa.generateTemplateProgram(program), outputFolder);
        }
    }

    public record ProgramExpression(int expressionIndex, Program program) {
    }

    private static Map<authoringassistant.paragraph.ExpressionCategory, List<ProgramExpression>> groupProgramsByCategory(
            List<Program> programs) {
        Map<authoringassistant.paragraph.ExpressionCategory, List<ProgramExpression>> categoryMap = new HashMap<>();

        for (Program program : programs) {
            List<authoringassistant.paragraph.TextFragment> fragments = program.getParagraph();
            for (int i = 0; i < fragments.size(); i++) {
                authoringassistant.paragraph.TextFragment fragment = fragments.get(i);
                if (fragment instanceof authoringassistant.paragraph.Expression expr) {
                    for (authoringassistant.paragraph.ExpressionCategory category : expr.getCategories()) {
                        categoryMap.computeIfAbsent(category, k -> new ArrayList<>())
                                .add(new ProgramExpression(i, program));
                    }
                }
            }
        }

        return categoryMap;
    }

    private static List<Program> downsamplePrograms(List<Program> programs, int expressionsPerCategory,
            int sampleSize) {
        Random random = new Random(0);
        Map<authoringassistant.paragraph.ExpressionCategory, List<ProgramExpression>> expressionsByCategory = groupProgramsByCategory(
                programs);
        Map<String, Program> selectedProgramsByTestFile = new HashMap<>();

        for (List<ProgramExpression> categoryExpressions : expressionsByCategory.values()) {
            List<ProgramExpression> shuffled = new ArrayList<>(categoryExpressions);
            Collections.shuffle(shuffled, random);
            int toTake = Math.min(expressionsPerCategory, shuffled.size());
            for (int i = 0; i < toTake; i++) {
                ProgramExpression pe = shuffled.get(i);
                selectedProgramsByTestFile.putIfAbsent(pe.program().getTestCaseFileName(), pe.program());
            }
        }
        Set<Program> downsampled = new HashSet<>(selectedProgramsByTestFile.values());

        // If we still have room, add more programs randomly
        if (downsampled.size() < sampleSize) {
            List<Program> remaining = new ArrayList<>(programs);
            remaining.removeAll(downsampled);
            Collections.shuffle(remaining, random);
            int additionalNeeded = Math.min(sampleSize - downsampled.size(), remaining.size());
            downsampled.addAll(remaining.subList(0, additionalNeeded));
        }

        // Convert to list and trim if exceeded maxPrograms
        List<Program> result = new ArrayList<>(downsampled);
        if (result.size() > sampleSize) {
            Collections.shuffle(result, random);
            result = result.subList(0, sampleSize);
        }

        return result;
    }

    private static void writeLog(ArrayList<Pair<Program, QueryResult>> results, String interpretationAgent, int learningContextSize) throws IOException {
        Files.createDirectories(Path.of(STR."results/\{Settings.getTestCaseFolder()}/"));
        try (PrintWriter out = new PrintWriter(new FileOutputStream(STR."results/\{Settings.getTestCaseFolder()}/results.csv"))) {
            String[] headers = {
                    "runId", "test-case", "llm-agent", "temperature", "num-token", "is-negative", "in-context-learning-size",
                    "attempts", "result", "target-value", "expression-type", "generated-expression", "expected-value", "expected-expression", "parseErrors", "counterfactualFails", "nullExpressions", "onlyLiteralExpressions", "duration(ms)"
            };
            out.println(String.join(";", headers));
            String content = results.stream()
                    .map(result -> {
                        Program program = result.getFirst();
                        QueryResult queryResult = result.getSecond();
                        String[] values = {
                                String.valueOf(queryResult.runId()),
                                STR."\{Path.of(result.getFirst().getTestCaseFileName()).getParent().getFileName()}/\{Path.of(result.getFirst().getTestCaseFileName()).getFileName()}",
                                interpretationAgent,
                                String.valueOf(Settings.getTemperature()),
                                String.valueOf(Settings.getNumContextToken()),
                                String.valueOf(result.getFirst().getTestCaseFileName().contains("negative")),
                                String.valueOf(learningContextSize),
                                //program.getParagraph().toFluidSyntax(),
                                String.valueOf(queryResult.attempt()),
                                queryResult.correctResponse() != null ? "OK" : "KO",
                                String.valueOf(Settings.isAddExpectedValueEnabled() ? 1 : 0),
                                STR."[\{queryResult.expected().getCategories().stream().map(cat -> cat.label).collect(Collectors.joining(","))}]",
//                                queryResult.correctResponse() != null ? "generated" : "NULL",
                                queryResult.correctResponse() != null ? queryResult.correctResponse().getExpr().replaceAll("\n", "[NEWLINE]").replaceAll("\"", "\"\"") : "NULL",
                                queryResult.expected().getValue(),
                                queryResult.expected().getExpr().replaceAll("\n", "[NEWLINE]").replaceAll("\"", "\"\""),
                                String.valueOf(queryResult.parseErrors()),
                                String.valueOf(queryResult.counterfactualFails()),
                                String.valueOf(queryResult.nullExpressions()),
                                String.valueOf(queryResult.onlyLiteralExpressions()),
                                String.valueOf(queryResult.duration())
                        };
                        return String.join(";", Arrays.stream(values).map(s -> STR."\"\{s}\"").toList());
                    })
                    .collect(Collectors.joining("\n"));
            out.println(content);
        }
    }

    private static float computeAccuracy(List<Pair<Program, QueryResult>> results) {
        logger.config("Computing accuracy");
        long count = IntStream.range(0, results.size()).filter(i -> {
            QueryResult result = results.get(i).getSecond();
            return result.correctResponse() != null
                    && result.expected().getExpr().equals(result.correctResponse().getExpr());
        }).count();
        return (float) count / results.size();
    }

    private static ArrayList<Pair<Program, QueryResult>> execute(InContextLearning inContextLearning, String interpretationAgent, String suggestionAgent, List<Program> programs) throws Exception {
        final ArrayList<Pair<Program, QueryResult>> allResults = new ArrayList<>();

        for(int k = 0; k < Settings.numTestRuns(); k++)
        {
            String jsonLogFolder = STR."\{Settings.LOG_FOLDER}/json_\{interpretationAgent}_\{k}_\{System.currentTimeMillis()}/";
            Files.createDirectories(Paths.get(jsonLogFolder));
            int programCount = 0;
            for (Program program : programs) {
                AuthoringAssistant authoringAssistant = new AuthoringAssistant(inContextLearning, interpretationAgent, program, suggestionAgent, k,jsonLogFolder);
                List<Pair<Program, QueryResult>> results = authoringAssistant.runTestProblems();

                long correct = results.stream()
                    .filter(r -> r.getSecond().correctResponse() != null)
                    .count();
                logger.info(STR."[Test case \{programCount++} of \{programs.size()}] \{correct} of \{results.size()} responses correct");
                allResults.addAll(results);
            }
        }
        return allResults;
    }

    public static Map<String, String> parseArguments(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.contains("="))
                .map(arg -> arg.split("=", 2))
                .filter(keyValue -> keyValue.length == 2)
                .collect(Collectors.toMap(
                        keyValue -> keyValue[0],
                        keyValue -> keyValue[1],
                        (_, replacement) -> replacement));
    }

    public static void generateLinks() throws Exception {
        String path = "website/authoring-assistant";
        File htmlFile = new File(STR."\{path}/template-index.html");


        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            String links = paths
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> !name.equals("datasets") && !name.equals("fluid") && !name.equals("css") && !name.equals("shared"))
                    .sorted()
                    .map(name -> STR."<a href=\"\{name}\">\{name}</a> <br />")
                    .collect(Collectors.joining("\n"));
            String html = new String(Files.readAllBytes(htmlFile.toPath()));
            html = html.replaceAll("##LINKS##", links);
            FileWriter file = new FileWriter(STR."\{path}/index.html");
            file.write(html);
            file.flush();
            file.close();
        }

    }

}
