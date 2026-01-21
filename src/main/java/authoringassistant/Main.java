package authoringassistant;

import authoringassistant.llm.interpretation.DummyAgent;
import kotlin.Pair;
import authoringassistant.Program.QueryResult;
import authoringassistant.util.ThrowingConsumer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static authoringassistant.Program.cleanWebsiteFolders;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());
    private static FileHandler fileHandler;

    public static void main(String... args) {
        Map<String, String> arguments = parseArguments(args);
        initLogging();
        logger.config("Arguments passed from command line");
        logger.config(arguments.toString().replace(",", "\n"));
        final ArrayList<Program> programs;
        final SystemPrompt systemPrompt;

        try {
            Settings.init("settings/default.json", arguments);
            setupResultsFileLogger();
            logger.info("****************************************");
            logger.info(STR."Settings:");
            logger.info(Settings.getSettings().toString(2));
            final String interpretationAgent = Settings.getAuthoringAgentName();
            final String suggestionAgent = Settings.getSuggestionAgentName();
            //Create directory for logs and json
            systemPrompt = SystemPrompt.load(Settings.SYSTEM_PROMPT_PATH);
            logger.info("****************************************");
            logger.info(STR."Validating test cases in \{Settings.getTestCaseFolder()}");
            logger.info("****************************************");
            programs = Program.loadPrograms(Settings.getTestCaseFolder());
            logger.info("****************************************");
            logger.info(STR."Validated test cases in \{Settings.getTestCaseFolder()}");
            logger.info("****************************************");
            if(suggestionAgent != null && interpretationAgent == null) {
                SuggestionAgent.generatePrograms(programs, suggestionAgent, STR."testCases/\{Settings.getTestCaseFolder()}-SuggestionAgent");
            }
            else if(arguments.containsKey("downsample") && arguments.get("downsample").equals("true")) {
                int expressionPerCategory = Integer.parseInt(arguments.get("expression-per-category"));
                int sampleSize = Integer.parseInt(arguments.get("sample-size"));
                String outputFolder = STR."testCases/\{Settings.getTestCaseFolder()}-downsampled";
                downsamplePrograms(programs, expressionPerCategory, sampleSize)
                    .forEach(ThrowingConsumer.toConsumer(program -> program.saveProgramToJson(outputFolder)));
            }
            else
            {
                cleanWebsiteFolders(STR."website/authoring-assistant/\{Settings.getTestCaseFolder()}/");
                final ArrayList<Pair<Program, QueryResult>> allResults = new ArrayList<>();
                boolean[] cases = isTestMock(interpretationAgent) ? new boolean[]{false} : new boolean[]{false, true};
                // Run experiment for both add-target-value settings
                for (boolean addExpectedValue : cases) {
                    Settings.setAddExpectedValue(addExpectedValue);
                    System.out.println(STR."Running experiment with add-target-value=\{addExpectedValue}");
                    final ArrayList<Pair<Program, QueryResult>> results = runTestCases(systemPrompt, interpretationAgent, suggestionAgent, programs);
                    allResults.addAll(results);
                }

                generateLinks();
                writeResults(allResults);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (fileHandler != null) {
                fileHandler.flush();
                fileHandler.close();
            }
        }
    }

    private static boolean isTestMock(String interpretationAgent) {
        return interpretationAgent.equals(DummyAgent.class.getName());
    }
    
    private static void initLogging() {
        try (FileInputStream in = new FileInputStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            System.err.println("Could not load logging.properties: " + e.getMessage());
        }
    }

    private static void setupResultsFileLogger() throws Exception {
        String interpretationAgent = Settings.getAuthoringAgentName();
        String modelName = interpretationAgent != null ? 
            authoringassistant.llm.LLMEvaluatorAgent.initialiseAgent(interpretationAgent).getModel() : "unknown";
        String resultsPath = STR."results/\{Settings.getConfigName()}/\{modelName}/\{Settings.getTestCaseFolder()}";
        Files.createDirectories(Path.of(resultsPath));
        fileHandler = new FileHandler(STR."\{resultsPath}/log.txt", /* append = */ false);
        fileHandler.setFormatter(new SimpleFormatter());
        Logger.getLogger("").addHandler(fileHandler);
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
                selectedProgramsByTestFile.putIfAbsent(pe.program().getTestCasePath().toString(), pe.program());
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

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static void writeResults(ArrayList<Pair<Program, QueryResult>> results) throws IOException {
        if (results.isEmpty()) {
            logger.warning("No results to write");
            return;
        }
        String modelName = results.get(0).getSecond().model();
        Files.createDirectories(Path.of(STR."results/\{Settings.getConfigName()}/\{modelName}/\{Settings.getTestCaseFolder()}"));
        try (PrintWriter out = new PrintWriter(new FileOutputStream(STR."results/\{Settings.getConfigName()}/\{modelName}/\{Settings.getTestCaseFolder()}/results.csv"))) {
            String[] headers = {
                    "run", "test-case", "problem-no", "llm-agent", "target-value-present", "categories",
                    "fails-interpreter", "fails-counterfactual", "fails-no-response", "fails-literal"
            };
            out.println(String.join(";", headers));
            String content = results.stream()
                    .map(result -> {
                        QueryResult queryResult = result.getSecond();

                        String[] values = {
                                String.valueOf(queryResult.runId()),
                                quote(STR."\{result.getFirst().getTestCasePath().getFileName()}"),
                                String.valueOf(queryResult.problemIndex()),
                                quote(queryResult.model()),
                                String.valueOf(Settings.isAddExpectedValue() ? 1 : 0),
                                quote(STR."[\{queryResult.expected().getCategories().stream()
                                        .map(cat -> cat.label)
                                        .collect(Collectors.joining(","))}]"),
                                String.valueOf(queryResult.parseErrors()),
                                String.valueOf(queryResult.counterfactualFails()),
                                String.valueOf(queryResult.missingResponses()),
                                String.valueOf(queryResult.literalResponses())
                        };

                        return String.join(";", values);
                    }).collect(Collectors.joining("\n"));
            out.println(content);
        }
    }

    private static ArrayList<Pair<Program, QueryResult>> runTestCases(SystemPrompt systemPrompt, String interpretationAgent, String suggestionAgent, List<Program> testCases) throws Exception {
        final ArrayList<Pair<Program, QueryResult>> allResults = new ArrayList<>();
        final int numRuns = isTestMock(interpretationAgent) ? 1 : Settings.numTestRuns();

        if (Settings.getTruncateTestsAt() != -1) {
            testCases = testCases.subList(0, Settings.getTruncateTestsAt());
        }

        for(int k = 0; k < numRuns; k++)
        {
            String jsonLogFolder = STR."\{Settings.LOG_FOLDER}\{Settings.getConfigName()}/json_\{interpretationAgent}_\{k}_\{System.currentTimeMillis()}/";
            Files.createDirectories(Paths.get(jsonLogFolder));
            int n = 0;
            for (Program testCase : testCases) {
                AuthoringAssistant authoringAssistant = new AuthoringAssistant(systemPrompt, interpretationAgent, testCase, suggestionAgent, k);
                List<Pair<Program, QueryResult>> results = authoringAssistant.runTestProblems();

                long correct = results.stream()
                    .filter(r -> r.getSecond().correctResponse() != null)
                    .count();
                n++;
                logger.info(STR."[Run \{k + 1} of \{numRuns}][Test case \{n} of \{testCases.size()}] \{correct} of \{results.size()} responses correct");
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
