package explorableviz.transparenttext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {
        Map<String, String> arguments = parseArguments(args);
        logger.info("Arguments passed from command line");
        logger.info(arguments.toString().replace(",", "\n"));
        final ArrayList<Program> programs;
        final InContextLearning inContextLearning;
        final String agent = arguments.get("agent");
        try {
            Settings.init("settings.json");
            inContextLearning = InContextLearning.loadLearningCases(Settings.getSystemPromptPath(), Settings.getNumLearningCaseToGenerate());
            programs = Program.loadPrograms(Settings.getTestCaseFolder(), Settings.getNumTestToGenerate());
            final int queryLimit = Settings.getNumQueryToExecute().orElseGet(programs::size);
            final ArrayList<QueryResult> results = execute(inContextLearning, agent, queryLimit, programs);
            float accuracy = computeAccuracy(results);
            writeLog(results, agent, inContextLearning.size());
            if (accuracy >= Settings.getThreshold()) {
                System.out.println(STR."Accuracy OK =\{accuracy}");
                System.exit(0);
            } else {
                System.out.println(STR."Accuracy KO =\{accuracy}");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static void writeLog(ArrayList<QueryResult> results, String agent, int learningContextSize) throws IOException {
        Files.createDirectories(Paths.get(Settings.getLogFolder()));
        try (PrintWriter out = new PrintWriter(new FileOutputStream(STR."\{Settings.getLogFolder()}/log_\{System.currentTimeMillis()}.csv"))) {
            String[] headers = {
                    "test-case", "llm-agent", "temperature", "num-token", "in-context-learning-size", "query-paragraph",
                    "attempts", "result", "generated-expression", "expected-expression", "expected-value", "duration(ms)"
            };
            out.println(String.join(";", headers));
            String content = results.stream()
                    .map(result -> {
                        String[] values = {
                                result.query().program().getTestCaseFileName(),
                                agent,
                                String.valueOf(Settings.getTemperature()),
                                String.valueOf(Settings.getNumContextToken()),
                                String.valueOf(learningContextSize),
                                result.query().paragraph(),
                                String.valueOf(result.attempt()),
                                result.response() != null ? "OK" : "KO",
                                String.valueOf(result.response() != null ? result.response().getExpr() : "NULL"),
                                result.query().expression().getExpr(),
                                result.query().expression().getValue(),
                                String.valueOf(result.duration())
                        };
                        return String.join(";", Arrays.stream(values).map(s -> STR."\"\{s}\"").toList());
                    })
                    .collect(Collectors.joining("\n"));
            out.println(content);
        }
    }

    private static float computeAccuracy(List<QueryResult> results) {
        logger.info("Computing accuracy");
        long count = IntStream.range(0, results.size()).filter(i -> results.get(i).response() != null && results.get(i).query().expression().getExpr().equals(results.get(i).response().getExpr())).count();
        return (float) count / results.size();
    }

    private static ArrayList<QueryResult> execute(InContextLearning inContextLearning, String agent, int programLimit, List<Program> programs) throws Exception {
        final ArrayList<QueryResult> results = new ArrayList<>();
        for (int i = 0; i < programLimit; i++) {
            AuthoringAssistant workflow = new AuthoringAssistant(inContextLearning, agent, programs.get(i));
            logger.info(STR."Analysing program id=\{i}");
            results.addAll(workflow.executeProgram());
        }
        logger.info("Printing generated expression");
        for (QueryResult result : results) {
            logger.info(result.response() != null ? result.response().getExpr() : "NULL");
        }
        return results;
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
}
