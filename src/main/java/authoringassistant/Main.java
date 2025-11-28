package authoringassistant;

import kotlin.Pair;

import authoringassistant.Program.QueryResult;

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
            Settings.init("settings.json");
            final String agent = Settings.getAuthoringAgentName();
            final String suggestionAgent = Settings.getSuggestionAgentName();
            //Create directory for logs and json
            cleanWebsiteFolders("website/authoring-assistant/");
            inContextLearning = InContextLearning.loadLearningCases(Settings.getSystemPromptPath(), Settings.getNumLearningCaseToGenerate());
            programs = Program.loadPrograms(Settings.getTestCaseFolder(), Settings.maxProgramVariants());
            if(arguments.containsKey("suggestion-agent-only") && arguments.get("suggestion-agent-only").equals("true")) {
                generatePrograms(programs, suggestionAgent, "testCases/scigen-SuggestionAgent");
                return;
            }
            else
            {
                final ArrayList<Pair<Program, QueryResult>> results = execute(inContextLearning, agent, suggestionAgent, programs);
                float accuracy = computeAccuracy(results);
                generateLinks();
                writeLog(results, agent, inContextLearning.size());
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
    private static void generatePrograms(List<Program> programs, String suggestionAgentClassName, String outputFolder) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        SuggestionAgent sa = new SuggestionAgent(suggestionAgentClassName);
        Files.createDirectories(Paths.get(outputFolder));

        for (Program program : programs) {
            Program p = sa.generateTemplateProgram(program);
            String json = p.toJsonProgram().toString(2);

            String fileName = STR."\{Path.of(p.getTestCaseFileName()).getFileName()}.json";
            Path outputPath = Paths.get(outputFolder, fileName);
            Files.writeString(outputPath, json);
            logger.info(STR."Generated program saved to: \{outputPath}");
        }
    }

    private static void writeLog(ArrayList<Pair<Program, QueryResult>> results, String agent, int learningContextSize) throws IOException {
        Files.createDirectories(Path.of(STR."results/\{Path.of(Settings.getTestCaseFolder()).getFileName()}/"));
        try (PrintWriter out = new PrintWriter(new FileOutputStream(STR."results/\{Path.of(Settings.getTestCaseFolder()).getFileName()}/results.csv"))) {
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
                                result.getFirst().getTestCaseFileName(),
                                agent,
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
            return  result.correctResponse() != null && result.expected().getExpr().equals(result.correctResponse().getExpr());
        }).count();
        return (float) count / results.size();
    }

    private static ArrayList<Pair<Program, QueryResult>> execute(InContextLearning inContextLearning, String agent, String suggestionAgent, List<Program> programs) throws Exception {
        final ArrayList<Pair<Program, QueryResult>> allResults = new ArrayList<>();

        for(int k = 0; k < Settings.numTestRuns(); k++)
        {
            String jsonLogFolder = STR."\{Settings.getLogFolder()}/json_\{agent}_\{k}_\{System.currentTimeMillis()}/";
            Files.createDirectories(Paths.get(jsonLogFolder));
            int programCount = 0;
            for (Program program : programs) {
                AuthoringAssistant authoringAssistant = new AuthoringAssistant(inContextLearning, agent, program, suggestionAgent, k,jsonLogFolder);
                List<Pair<Program, QueryResult>> results = authoringAssistant.runTestProblems();

                long correct = results.stream()
                    .filter(r -> r.getSecond().correctResponse() != null)
                    .count();
                logger.info(STR."Test case \{programCount++} of \{programs.size()}: \{correct} of \{results.size()} responses correct");
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
                        (_, replacement) -> replacement
                ));
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
