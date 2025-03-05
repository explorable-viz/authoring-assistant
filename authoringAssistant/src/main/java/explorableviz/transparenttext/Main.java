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
        final ArrayList<Query> queries;
        final LearningQuery learningQuery;
        final String agent = arguments.get("agent");
        try {
            Settings.init("settings.json");
            learningQuery = LearningQuery.importLearningCaseFromJSON(Settings.getSystemPromptPath(), Settings.getNumLearningCaseToGenerate());
            queries = TestQuery.loadCases(Settings.getTestCaseFolder(), Settings.getNumTestToGenerate());
            final int queryLimit = Settings.getNumQueryToExecute().orElseGet(queries::size);
            final ArrayList<QueryResult> results = execute(learningQuery, agent, queryLimit, queries);
            float accuracy = computeAccuracy(results, queries, queryLimit);
            writeLog(results, agent, learningQuery.size());
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
                    "test-case", "llm-agent", "temperature", "num-token", "in-context-learning-size",
                    "attempts", "result", "generated-expression", "expected", "duration(ms)"
            };
            out.println(String.join(";", headers));
            String content = results.stream()
                    .map(result -> {
                        String[] values = {
                                result.query().getTestCaseFileName(),
                                agent,
                                String.valueOf(Settings.getTemperature()),
                                String.valueOf(Settings.getNumContextToken()),
                                String.valueOf(learningContextSize),
                                String.valueOf(result.attempt()),
                                result.response() != null ? "OK" : "KO",
                                String.valueOf(result.response()),
                                result.query().getExpected(),
                                String.valueOf(result.duration())
                        };
                        assert headers.length == values.length :
                                STR."Mismatch: headers=\{headers.length}, values=\{values.length}";
                        return String.join(";", values);
                    })
                    .collect(Collectors.joining("\n"));
            out.println(content);
        }
    }

    private static float computeAccuracy(List<QueryResult> results, List<Query> queries, int queryLimit) {
        logger.info("Computing accuracy");
        long count = IntStream.range(0, results.size()).filter(i -> {
            logger.info(STR."I=\{i}exp=\{queries.get(i).getExpected()} obtained=\{results.get(i).response()}");
            return queries.get(i).getExpected().equals(results.get(i).response());
        }).count();
        return (float) count / queryLimit;
    }

    private static ArrayList<QueryResult> execute(LearningQuery learningQuery, String agent, int queryLimit, ArrayList<Query> queries) throws Exception {
        final ArrayList<QueryResult> results = new ArrayList<>();
        AuthoringAssistant workflow = new AuthoringAssistant(learningQuery, agent);
        for (int i = 0; i < queryLimit; i++) {
            logger.info(STR."Analysing query id=\{i}");
            results.add(workflow.execute(queries.get(i)));
        }
        logger.info("Printing generated expression");
        for (QueryResult result : results) {
            logger.info(result.response());
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
